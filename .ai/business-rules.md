# Regras de Negócio — HelpDesk Lite

Domínio de chamados internos (TI, Facilities, RH). Este arquivo é a fonte da verdade do
comportamento do sistema. Toda regra aqui é validada **no backend**; o frontend apenas
esconde botões (isso é UX, não segurança).

---

## 1. Enums

### `Perfil`
| Valor | Quem é |
|---|---|
| `SOLICITANTE` | Abre chamados e acompanha os próprios |
| `ATENDENTE` | Assume e resolve chamados |
| `ADMIN` | Tudo o que os outros fazem, mais cadastros e dashboard |

### `StatusChamado`
| Valor | Significado |
|---|---|
| `ABERTO` | Criado, ainda sem atendente |
| `EM_ATENDIMENTO` | Um atendente assumiu (ou o chamado foi reaberto) |
| `RESOLVIDO` | O atendente concluiu; aguarda confirmação do solicitante |
| `FECHADO` | O solicitante confirmou a resolução. Estado final |

### `Prioridade`
| Valor |
|---|
| `BAIXA` |
| `MEDIA` |
| `ALTA` |

---

## 2. Tabela de permissões (ação × perfil)

Cópia fiel da seção 3 do documento de arquitetura.

| Ação | Solicitante | Atendente | Admin |
|---|:---:|:---:|:---:|
| Abrir chamado | ✅ | ✅ | ✅ |
| Ver próprios chamados | ✅ | ✅ | ✅ |
| Ver todos os chamados | ❌ | ✅ | ✅ |
| Assumir chamado | ❌ | ✅ | ✅ |
| Alterar status (em atendimento / resolvido) | ❌ | ✅ (só os seus) | ✅ |
| Fechar chamado (confirmar resolução) | ✅ (só os seus) | ❌ | ✅ |
| Comentar | ✅ (só os seus) | ✅ | ✅ |
| Gerenciar categorias | ❌ | ❌ | ✅ |
| Gerenciar usuários | ❌ | ❌ | ✅ |
| Ver dashboard | ❌ | ✅ | ✅ |

**Leitura de "só os seus":**

- Para o **solicitante**, "seu" = `chamado.solicitante.id == usuarioAtual.id`.
- Para o **atendente**, "seu" = `chamado.atendente.id == usuarioAtual.id`
  (o chamado que ele assumiu, não o que ele abriu).

O `ADMIN` nunca é limitado pela relação com o chamado.

### Tradução da tabela para `PermissaoService`

| Método | Regra |
|---|---|
| `verificarAbertura(usuario)` | Qualquer perfil ativo. Nunca nega |
| `verificarVisualizacao(usuario, chamado)` | `ADMIN`/`ATENDENTE` veem qualquer um. `SOLICITANTE` só se for o solicitante do chamado, senão `AcessoNegadoException` |
| `verificarAssuncao(usuario, chamado)` | Só `ATENDENTE` ou `ADMIN` |
| `verificarAlteracaoStatus(usuario, chamado, novoStatus)` | Ver seção 3.3 |
| `verificarComentario(usuario, chamado)` | Mesma regra de `verificarVisualizacao` |
| `verificarGestaoCategorias(usuario)` | Só `ADMIN` |
| `verificarGestaoUsuarios(usuario)` | Só `ADMIN` |
| `verificarDashboard(usuario)` | Só `ATENDENTE` ou `ADMIN` |

---

## 3. Máquina de estados

### 3.1 Transições permitidas

Qualquer par `(de, para)` **fora desta tabela** — inclusive `X -> X` — é rejeitado com
**HTTP 422** e código de erro `TRANSICAO_INVALIDA`.

| De | Para | Perfis que executam | Como acontece |
|---|---|---|---|
| `ABERTO` | `EM_ATENDIMENTO` | `ATENDENTE`, `ADMIN` | `PATCH /chamados/{id}/assumir` |
| `EM_ATENDIMENTO` | `RESOLVIDO` | `ATENDENTE`, `ADMIN` | `PATCH /chamados/{id}/status` |
| `RESOLVIDO` | `FECHADO` | `SOLICITANTE`, `ADMIN` | `PATCH /chamados/{id}/status` |
| `RESOLVIDO` | `EM_ATENDIMENTO` | `SOLICITANTE`, `ADMIN` | `PATCH /chamados/{id}/status` (reabertura) |

Consequências diretas da tabela:

- `FECHADO` é **estado final**: não sai dele por nenhum caminho.
- **Reabertura só sai de `RESOLVIDO`.** Não existe `FECHADO -> EM_ATENDIMENTO`.
- Não existe atalho `ABERTO -> RESOLVIDO` nem `ABERTO -> FECHADO`.
- Um chamado já assumido não pode ser assumido de novo: ele já está em
  `EM_ATENDIMENTO`, e `EM_ATENDIMENTO -> EM_ATENDIMENTO` não está na tabela.

Esta tabela vive em **um único lugar**: `domain/state/TransicaoStatus`. Nenhum service
tem `if` sobre status.

### 3.2 Ordem obrigatória de verificação

Em toda operação sobre um chamado, nesta ordem e sem exceção:

1. **Existe?** -> senão `404 RECURSO_NAO_ENCONTRADO`
2. **Quem pode?** (`PermissaoService`, perfil + relação) -> senão `403 ACESSO_NEGADO`
3. **Pode agora?** (`TransicaoStatus`, estado atual) -> senão `422 TRANSICAO_INVALIDA`

Inverter 2 e 3 vazaria o estado do chamado para quem não pode vê-lo.

### 3.3 Divisão exata entre 403 e 422 na alteração de status

`PermissaoService.verificarAlteracaoStatus` responde "esse perfil, nessa relação, pode
**em algum momento** levar um chamado a esse status?" — sem olhar o status atual:

| Perfil | Status-alvo aceitos | Relação exigida |
|---|---|---|
| `ADMIN` | `EM_ATENDIMENTO`, `RESOLVIDO`, `FECHADO` | nenhuma |
| `ATENDENTE` | `RESOLVIDO` | ser o atendente do chamado |
| `SOLICITANTE` | `FECHADO`, `EM_ATENDIMENTO` (reabrir) | ser o solicitante do chamado |

Qualquer combinação fora disso -> **403**. Ninguém pode levar um chamado de volta para
`ABERTO`: alvo `ABERTO` é sempre 403.

`TransicaoStatus.validar(de, para, perfil)` responde "essa aresta existe e esse perfil a
executa?". Falhou -> **422**.

Exemplos que amarram a regra:

| Cenário | Resultado |
|---|---|
| Atendente resolve chamado `EM_ATENDIMENTO` que ele atende | 200 |
| Atendente resolve chamado que **outro** atendente atende | **403** (relação) — não revela o status |
| Atendente tenta fechar o próprio chamado atendido | **403** (alvo fora da lista dele) |
| Solicitante fecha o próprio chamado `RESOLVIDO` | 200 |
| Solicitante fecha o próprio chamado `ABERTO` | **422** (`ABERTO -> FECHADO` não existe) |
| Solicitante fecha chamado **de outro** | **403** |
| Solicitante reabre o próprio chamado `RESOLVIDO` | 200 |
| Solicitante reabre o próprio chamado `FECHADO` | **422** (reabertura só de `RESOLVIDO`) |
| Qualquer um tenta voltar para `ABERTO` | **403** |

### 3.4 Efeitos colaterais de cada transição

Todos os instantes vêm do `Clock` injetado — nunca de `Instant.now()`.

| Operação | Efeitos |
|---|---|
| Abrir | `status = ABERTO`, `criadoEm = agora`, `atualizadoEm = agora`, `atendente = null` |
| Assumir | `atendente = usuarioAtual`, `status = EM_ATENDIMENTO`, `atualizadoEm = agora` |
| Para `RESOLVIDO` | `resolvidoEm = agora`, `atualizadoEm = agora` |
| Para `FECHADO` | `fechadoEm = agora`, `atualizadoEm = agora` (mantém `resolvidoEm`) |
| Para `EM_ATENDIMENTO` (reabrir) | `resolvidoEm = null`, `fechadoEm = null`, `atualizadoEm = agora`; **mantém o atendente** |

Reabrir zera `resolvidoEm` porque o chamado voltou a estar em aberto: contá-lo como
resolvido distorceria o tempo médio de resolução e o cálculo de SLA.

---

## 4. Regras de integridade

| # | Regra | Violação |
|---|---|---|
| I1 | Todo chamado tem **exatamente um** solicitante e **uma** categoria | 400 na validação do DTO / 404 se o id não existe |
| I2 | `atendente` é `null` até alguém assumir; só é preenchido no `assumir` | — |
| I3 | Só `ATENDENTE` ou `ADMIN` podem ser atendentes de um chamado | 403 |
| I4 | **Comentário é imutável**: não existe endpoint de edição nem de exclusão | rota inexistente |
| I5 | Categoria **não pode ser excluída** se houver chamados vinculados — só desativada (`ativa = false`) | O `DELETE` é sempre soft delete, então nunca falha por isso |
| I6 | Usuário é desativado, nunca apagado (`ativo = false`) | idem |
| I7 | `email` de usuário é **único**; `nome` de categoria é **único** | 422 `REGRA_NEGOCIO` |
| I8 | Um chamado só pode ser aberto em categoria **ativa** | 422 `REGRA_NEGOCIO` |
| I9 | Usuário **inativo** não autentica | 401 `NAO_AUTENTICADO` |
| I10 | Não é possível desativar o **próprio** usuário logado | 422 `REGRA_NEGOCIO` |

Por que soft delete (I5/I6): o chamado histórico precisa continuar referenciando quem o
abriu e em que categoria estava. Hard delete com cascata apagaria histórico.

---

## 5. Visibilidade

**Regra única, aplicada em todo endpoint de leitura:** o `SOLICITANTE` só enxerga os
chamados em que ele é o solicitante.

| Onde | Como |
|---|---|
| `GET /chamados` (lista) | `ChamadoSpecification` adiciona `solicitante_id = :usuarioAtual` ao `WHERE` quando o perfil é `SOLICITANTE` |
| `GET /chamados/{id}` | `PermissaoService.verificarVisualizacao` -> 403 |
| `GET /chamados/{id}/comentarios` | idem |
| `POST /chamados/{id}/comentarios` | idem |
| `GET /dashboard/resumo` | Solicitante não acessa o dashboard (403) |

A visibilidade da lista é aplicada **na query, nunca filtrando em memória**: assim nem a
paginação nem o `totalElements` vazam a existência de chamados alheios.

Filtros disponíveis na listagem, todos opcionais e combináveis:
`status`, `prioridade`, `categoriaId` e `meus=true`.

`meus=true` significa:

| Perfil | Efeito de `meus=true` |
|---|---|
| `SOLICITANTE` | Nenhum na prática (já só vê os próprios) |
| `ATENDENTE` / `ADMIN` | `atendente_id = usuarioAtual` — a fila pessoal dele |

Paginação padrão em todas as listagens: `page=0`, `size=20`, `sort=criadoEm,desc`.

---

## 6. SLA e dashboard

### 6.1 Fórmulas (seção 8.3 do documento)

```
prazo(chamado) = chamado.criadoEm + chamado.categoria.slaHoras (em horas)

foraDoSla(chamado) =
      (status em {ABERTO, EM_ATENDIMENTO} E agora > prazo)
   OU (resolvidoEm != null E resolvidoEm > prazo)

tempoMedioResolucao = média(resolvidoEm - criadoEm)
                      sobre os chamados com status em {RESOLVIDO, FECHADO}
```

Detalhes que fecham a fórmula:

- "Agora" e o `Clock` são os mesmos do resto do sistema (UTC).
- Um chamado `RESOLVIDO`/`FECHADO` entregue **dentro** do prazo **não** está fora do SLA,
  mesmo que hoje já seja depois do prazo — a segunda cláusula olha `resolvidoEm`,
  não `agora`.
- Um chamado `FECHADO` carrega o `resolvidoEm` da época em que foi resolvido, então
  entra no tempo médio.
- Chamado sem `resolvidoEm` (aberto ou em atendimento) **não** entra no tempo médio.
- Sem nenhum chamado resolvido, o tempo médio é **zero**, nunca nulo nem divisão por zero.
- A comparação é estrita (`>`): estar exatamente no prazo ainda está dentro do SLA.

### 6.2 Conteúdo de `GET /dashboard/resumo`

| Campo | Conteúdo |
|---|---|
| `totalPorStatus` | Mapa `StatusChamado -> quantidade`, com **todos** os status presentes (zero inclusive) |
| `totalPorPrioridade` | Mapa `Prioridade -> quantidade`, todas as prioridades presentes |
| `totalChamados` | Total geral |
| `foraDoSla` | Quantidade de chamados fora do SLA agora |
| `tempoMedioResolucaoHoras` | Tempo médio em horas, arredondado a 2 casas |
| `chamadosForaDoSla` | Lista (resumo) dos chamados fora do SLA, ordenada do mais atrasado para o menos |

O dashboard é agregado sobre **todos** os chamados: só `ATENDENTE` e `ADMIN` acessam,
e ambos enxergam a base inteira.

---

## 7. Observabilidade das regras

Toda transição de status e toda ação administrativa (CRUD de usuário e de categoria)
grava **uma linha `INFO`** estruturada, com: `requestId`, `usuarioId`, `perfil`, `acao`,
`chamadoId`, `statusDe`, `statusPara`. É isso que reconstrói a história de um chamado
sem tabela de auditoria no MVP.

Métricas de negócio emitidas (sempre via a classe `MetricasNegocio`):

| Métrica | Tipo | Tags | Quando |
|---|---|---|---|
| `helpdesk_chamados_abertos_total` | Counter | `categoria`, `prioridade` | `ChamadoService.abrir` |
| `helpdesk_chamados_transicoes_total` | Counter | `de`, `para`, `perfil` | `assumir` e `alterarStatus` |
| `helpdesk_chamados_por_status` | Gauge | `status` | Recalculado a cada leitura do dashboard |
| `helpdesk_chamados_fora_sla` | Gauge | — | Recalculado a cada leitura do dashboard |
| `helpdesk_tempo_resolucao_segundos` | Timer | `categoria` | Ao entrar em `RESOLVIDO` |

---

## 8. Padrão de erro

```json
{
  "timestamp": "2026-09-21T22:10:00Z",
  "status": 422,
  "erro": "TRANSICAO_INVALIDA",
  "mensagem": "Chamado ABERTO não pode ir direto para RESOLVIDO"
}
```

Em erro de validação (400) o corpo ganha o campo `campos`, com um item por campo inválido:

```json
{
  "timestamp": "2026-09-21T22:10:00Z",
  "status": 400,
  "erro": "VALIDACAO",
  "mensagem": "Requisição inválida",
  "campos": [ { "campo": "titulo", "mensagem": "não deve estar em branco" } ]
}
```

| HTTP | `erro` | Quando |
|---|---|---|
| 400 | `VALIDACAO` | Bean Validation, JSON malformado, tipo de parâmetro errado |
| 401 | `NAO_AUTENTICADO` | `X-User-Id` ausente, não numérico, inexistente ou de usuário inativo |
| 403 | `ACESSO_NEGADO` | Perfil ou relação não permite a ação |
| 404 | `RECURSO_NAO_ENCONTRADO` | Id inexistente |
| 422 | `TRANSICAO_INVALIDA` | Transição fora da máquina de estados |
| 422 | `REGRA_NEGOCIO` | Outra regra de domínio (nome duplicado, categoria inativa...) |
| 500 | `ERRO_INTERNO` | Qualquer exceção não prevista. Nunca vaza stack trace |

Mensagens de erro são **em português do Brasil** e voltadas ao usuário final.

---

## 9. Seed obrigatório (`data.sql`)

O seed é carregado a cada `mvn spring-boot:run` (H2 em memória). Ele é **fixture de
demonstração** e nunca deve ser reaproveitado por teste automatizado — testes usam os
builders de `src/test/java/br/com/helpdesk/support/`.

### 9.1 Usuários — um de cada perfil

| id | nome | email | perfil | ativo |
|---|---|---|---|---|
| 1 | Ana Solicitante | ana@empresa.com | `SOLICITANTE` | true |
| 2 | Bruno Atendente | bruno@empresa.com | `ATENDENTE` | true |
| 3 | Carla Admin | carla@empresa.com | `ADMIN` | true |

### 9.2 Categorias — quatro, com SLAs diferentes

| id | nome | sla_horas | ativa |
|---|---|---|---|
| 1 | TI - Infraestrutura | 4 | true |
| 2 | TI - Sistemas | 8 | true |
| 3 | Facilities | 24 | true |
| 4 | RH | 48 | true |

### 9.3 Chamados — oito, cobrindo todos os status e prioridades

Pelo menos um **fora do SLA**. Os instantes são relativos ao momento em que a aplicação
sobe (`DATEADD` sobre `CURRENT_TIMESTAMP`), para que o cenário "fora do SLA" continue
verdadeiro em qualquer dia.

| id | título | status | prioridade | cat. | solic. | atend. | criado | resolvido | fechado | fora do SLA? |
|---|---|---|---|:---:|:---:|:---:|---|---|---|---|
| 1 | Notebook não liga | `ABERTO` | `ALTA` | 1 | 1 | — | -2 h | — | — | não (prazo 4 h) |
| 2 | Sem acesso ao ERP | `ABERTO` | `MEDIA` | 2 | 1 | — | **-30 h** | — | — | **SIM** (prazo 8 h) |
| 3 | Ar-condicionado com defeito | `ABERTO` | `BAIXA` | 3 | 3 | — | -6 h | — | — | não (prazo 24 h) |
| 4 | Erro ao emitir relatório | `EM_ATENDIMENTO` | `ALTA` | 2 | 1 | 2 | -5 h | — | — | não (prazo 8 h) |
| 5 | Troca de monitor | `EM_ATENDIMENTO` | `BAIXA` | 1 | 3 | 2 | **-20 h** | — | — | **SIM** (prazo 4 h) |
| 6 | Dúvida sobre folha de ponto | `RESOLVIDO` | `MEDIA` | 4 | 1 | 2 | -72 h | -50 h | — | não (prazo 48 h) |
| 7 | Cadeira quebrada | `RESOLVIDO` | `BAIXA` | 3 | 3 | 2 | **-96 h** | **-60 h** | — | **SIM** (resolvido depois do prazo de 24 h) |
| 8 | Instalar VPN | `FECHADO` | `ALTA` | 1 | 1 | 2 | -48 h | -46 h | -45 h | não (prazo 4 h) |

Cobertura garantida pelo seed: os 4 status, as 3 prioridades, as 4 categorias, os 3
usuários, chamados com e sem atendente, e 3 chamados fora do SLA — um resolvido tarde
(#7) e dois ainda em aberto (#2 e #5).

### 9.4 Comentários — seis

| id | chamado | autor | texto (resumo) | criado |
|---|:---:|:---:|---|---|
| 1 | 4 | 1 | Solicitante descreve o erro | -5 h |
| 2 | 4 | 2 | Atendente pede print | -4 h |
| 3 | 4 | 1 | Solicitante responde | -3 h |
| 4 | 6 | 2 | Atendente explica a regra | -51 h |
| 5 | 6 | 1 | Solicitante agradece | -50 h |
| 6 | 8 | 2 | Atendente registra a instalação | -46 h |

Os comentários se concentram nos chamados 4, 6 e 8 para que a demonstração tenha uma
linha do tempo de verdade em cada estágio do fluxo: em atendimento, resolvido e fechado.
