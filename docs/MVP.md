# HelpDesk Lite — guia do MVP

Como rodar, demonstrar e operar o MVP implementado. A apresentação do projeto e a equipe
estão no [README da raiz](../README.md).

Sistema web de chamados internos (TI, Facilities, RH). Solicitantes abrem chamados,
atendentes assumem e resolvem, administradores gerenciam cadastros e acompanham
indicadores de SLA.

MVP acadêmico da disciplina **AI Driven Development**: roda 100% local, com banco em
memória e dados de demonstração carregados no boot.

> Todos os comandos deste guia são executados **a partir da raiz do repositório**.

| | |
|---|---|
| **Backend** | Java 21 + Spring Boot 3.3 + Spring Data JPA + H2 — porta **8080** |
| **Frontend** | Vue 3 + Quasar 2 + Pinia + Axios — porta **9000** |
| **Autenticação** | Simulada: header `X-User-Id`, sem senha e sem token |
| **Tema** | Claro, escuro ou automático (segue o sistema), com a escolha lembrada |
| **Testes** | 151 no backend (JUnit 5 + Mockito + AssertJ), 49 no frontend (Vitest) |
| **Cobertura** | 94,8% de linhas em `domain` e `service` (mínimo exigido: 80%) |

---

## 1. Como está organizado

```
HelpDesk.Lite.FIAP/
├── README.md                # Apresentação do projeto e equipe
├── Atividade 1/             # Entregável da Aula 1: o documento de arquitetura
├── Atividade 2/             # Entregáveis da Aula 2: os prompts de contexto e implementação
├── .ai/                     # Contexto para agentes de IA — a "constituição" do projeto
│   ├── standards.md         #   Convenções de código e estilo
│   ├── architecture.md      #   Decisões de alto nível (18 ADRs)
│   ├── tech-stack.md        #   Versões e bibliotecas permitidas
│   └── business-rules.md    #   Lógica de negócio e domínio
├── docs/
│   ├── ARQUITETURA.md       # Documento de arquitetura — fonte da verdade
│   └── MVP.md               # Este guia
├── backend/                 # API REST Spring Boot
├── frontend/                # SPA Quasar
└── .gitignore
```

Os quatro arquivos em `.ai/` são derivados de `docs/ARQUITETURA.md` e foram a entrada do
agente que escreveu o código. Em caso de conflito, **`.ai/` prevalece sobre `docs/`**.

> `docs/ARQUITETURA.md` é o mesmo documento entregue em
> `Atividade 1/01_Arquitetura_HelpDesk_Lite v3.md`. Ele existe nos dois lugares de
> propósito: `Atividade 1/` preserva o entregável da aula como foi entregue, e `docs/` é o
> caminho que os arquivos de `.ai/` e este guia referenciam.

---

## 2. Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| **JDK** | 21 | Testado com Eclipse Temurin 21.0.12 |
| **Maven** | 3.9+ | Opcional — há um wrapper (`backend/mvnw`) que baixa o Maven sozinho |
| **Node.js** | 20 LTS ou superior | Validado em Node 24 LTS |

Nada além disso. Sem Docker, sem banco para instalar, sem variável de ambiente para
configurar.

Verifique com:

```bash
java -version && node -v
```

---

## 3. Como rodar

São **dois terminais**, um para cada processo.

### Terminal 1 — backend

```bash
cd backend && ./mvnw spring-boot:run
```

No Windows (PowerShell/cmd), use `.\mvnw.cmd spring-boot:run`. Com o Maven já instalado,
`mvn spring-boot:run` funciona igual.

Espere a linha `Started HelpdeskApplication`. A API fica em **http://localhost:8080**.

### Terminal 2 — frontend

```bash
cd frontend && npm install && npx quasar dev
```

O `npm install` só é necessário na primeira vez. A SPA abre em
**http://localhost:9000**.

> O backend libera CORS apenas para `http://localhost:9000`. Se você mudar a porta da
> SPA, ajuste `CorsConfig` no backend.

### Rodando os testes

```bash
cd backend && ./mvnw verify
```

```bash
cd frontend && npm run test
```

`./mvnw verify` roda os 151 testes **e** falha o build se a cobertura de `domain` e
`service` cair abaixo de 80%. O relatório HTML fica em
`backend/target/site/jacoco/index.html`.

---

## 4. Usuários de teste

Não existe senha. Na tela de login você escolhe com qual usuário entrar — é isso que
define o `X-User-Id` enviado em cada requisição.

| Id | Nome | E-mail | Perfil | O que consegue fazer |
|:--:|---|---|---|---|
| **1** | Ana Solicitante | ana@empresa.com | `SOLICITANTE` | Abre chamados, comenta e fecha **os próprios**. Só enxerga os chamados dela |
| **2** | Bruno Atendente | bruno@empresa.com | `ATENDENTE` | Vê todos os chamados, assume, resolve, comenta e acessa o dashboard |
| **3** | Carla Admin | carla@empresa.com | `ADMIN` | Tudo o que os outros fazem, mais os cadastros de categorias e usuários |

O seed também cria 4 categorias (SLAs de 4h, 8h, 24h e 48h), 8 chamados cobrindo todos os
status e prioridades — **3 deles fora do SLA** — e 6 comentários. Como o H2 é em memória,
**tudo volta ao estado inicial a cada restart do backend**.

---

## 5. O fluxo do sistema

```
ABERTO ──assume──► EM_ATENDIMENTO ──resolve──► RESOLVIDO ──confirma──► FECHADO
(atendente)          (atendente)                 (solicitante)
                            ▲                         │
                            └──────── reabre ─────────┘
                                    (solicitante)
```

Duas verificações independentes protegem cada operação, sempre nesta ordem:

1. **Quem pode?** — perfil + relação com o chamado → **403** se não pode.
2. **Pode agora?** — a transição existe a partir do status atual → **422** se não existe.

A ordem importa: um atendente que tenta mexer no chamado de outro recebe 403 e **não
descobre em que estado aquele chamado está**.

---

## 6. Roteiro de demonstração (3 a 5 minutos)

Roteiro pronto para gravar. Os tempos são sugestões.

### Antes de começar
- Backend e frontend rodando (seção 3).
- Abas prontas: SPA (`:9000`), Swagger (`:8080/swagger-ui.html`) e o terminal do backend
  visível, para mostrar os logs.

### 0:00 — 0:30 · Abertura
> "HelpDesk Lite: chamados internos, três perfis, back em Spring Boot e front em Quasar."

Mostre a tela de login com os três usuários. Explique em uma frase que a autenticação é
simulada — escolher o usuário define o header `X-User-Id`.

> Se quiser um detalhe visual rápido, o ícone no canto superior direito troca entre tema
> claro, escuro e automático. A escolha fica salva e vale para todas as telas.

### 0:30 — 1:15 · Solicitante abre um chamado
1. Entre como **Ana Solicitante**.
2. Note que a lista mostra **só os chamados dela** — não os 8 do seed.
3. **Novo chamado** → preencha título, descrição, categoria e prioridade.
4. Mostre o banner com o SLA da categoria escolhida.
5. Salve. O chamado nasce em **ABERTO**, sem atendente.
6. Repare que não há botão de ação: solicitante não assume nem resolve.

### 1:15 — 2:15 · Atendente assume e resolve
1. **Sair** → entre como **Bruno Atendente**.
2. A lista agora mostra **todos** os chamados. Filtre por `status = ABERTO`.
3. Abra o chamado recém-criado → **Assumir**. O status vira `EM_ATENDIMENTO` e o Bruno
   aparece como atendente.
4. Escreva um comentário — ele entra na linha do tempo.
5. **Resolver**. O campo "Resolvido em" aparece.
6. Mostre o **Dashboard**: cards por status, chamados fora do SLA e tempo médio de
   resolução.

### 2:15 — 3:00 · Solicitante confirma
1. Volte para **Ana**.
2. Abra o chamado: agora há **Confirmar e fechar** e **Reabrir**.
3. Clique em **Confirmar e fechar** → `FECHADO`, sem mais nenhuma ação disponível.

> Se sobrar tempo: em vez de fechar, clique em **Reabrir** e mostre que o chamado volta
> para `EM_ATENDIMENTO` mantendo o atendente.

### 3:00 — 3:40 · Admin e as regras
1. Entre como **Carla Admin**. O menu lateral ganha **Categorias** e **Usuários**.
2. Em **Categorias**, crie uma e desative outra — explique que desativar é *soft delete*,
   porque os chamados históricos continuam apontando para ela.
3. No Swagger, chame `PATCH /api/chamados/{id}/status` com `X-User-Id: 1` (Ana) e
   `{"status": "RESOLVIDO"}` → **403**: o solicitante não resolve.
4. Repita com `{"status": "FECHADO"}` em um chamado `ABERTO` → **422**: a transição não
   existe. Esses dois erros mostram a divisão "quem pode" x "pode agora".

### 3:40 — 4:30 · Observabilidade
1. Provoque um erro na interface (por exemplo o 403 acima) e mostre o `q-notify`: ele traz
   a mensagem do backend **e o request id**.
2. Copie esse id e rode no terminal:
   ```bash
   grep "<cole-o-request-id-aqui>" log-do-backend.txt
   ```
   Toda a requisição aparece correlacionada, incluindo a linha de auditoria
   `acao=ALTERAR_STATUS ... statusDe=... statusPara=...`.
3. Abra `http://localhost:8080/actuator/prometheus` e mostre as métricas `helpdesk_*`.
4. Feche mencionando `./mvnw verify` com cobertura acima de 80% e `npm run test`.

---

## 7. Operação

### URLs

| URL | O que é |
|---|---|
| http://localhost:9000 | SPA |
| http://localhost:8080/swagger-ui.html | **Swagger UI** — documentação viva e execução manual dos endpoints |
| http://localhost:8080/v3/api-docs | Especificação OpenAPI em JSON |
| http://localhost:8080/h2 | **Console do H2** |
| http://localhost:8080/actuator/health | Health check, com o indicador do banco |
| http://localhost:8080/actuator/metrics | Lista de métricas disponíveis |
| http://localhost:8080/actuator/prometheus | Métricas em formato Prometheus, incluindo as `helpdesk_*` |

**Para entrar no console H2:** JDBC URL `jdbc:h2:mem:helpdesk`, usuário `sa`, senha em
branco. O campo JDBC URL vem preenchido errado por padrão — troque para esse valor.

### Usando o Swagger

Todos os endpoints sob `/api`, exceto `/api/auth/**`, exigem o header `X-User-Id`. No
Swagger, ele aparece como um campo no próprio formulário de cada operação. **Trocar esse
número é como trocar de usuário logado** — é a forma mais rápida de demonstrar as regras
de permissão sem sair da tela.

```bash
# Ana (solicitante) vê só os chamados dela
curl -s -H "X-User-Id: 1" "http://localhost:8080/api/chamados" | grep -o '"totalElements":[0-9]*'

# Carla (admin) vê a base inteira
curl -s -H "X-User-Id: 3" "http://localhost:8080/api/chamados" | grep -o '"totalElements":[0-9]*'
```

### Achando tudo o que aconteceu em uma requisição

Toda resposta da API traz o header **`X-Request-Id`**. O mesmo id aparece em cada linha de
log daquela requisição — e é exibido no rodapé do `q-notify` de erro na interface, para
você conseguir pedir ao usuário "me manda o código que apareceu na tela".

O padrão de log é `[req=<requestId> usr=<usuarioId>]`:

```
23:39:18.070 INFO [nio-8080-exec-9] [req=a3f9... usr=2] b.c.h.service.ChamadoService
  : acao=ASSUMIR_CHAMADO chamadoId=1 perfil=ATENDENTE statusDe=ABERTO statusPara=EM_ATENDIMENTO
```

**Exemplo completo.** Envie um id conhecido e depois filtre o log por ele:

```bash
curl -i -X PATCH http://localhost:8080/api/chamados/1/assumir \
  -H "X-User-Id: 2" -H "X-Request-Id: RASTREIO-001"
```

Se o backend estiver rodando em primeiro plano, o id já aparece no terminal. Para filtrar,
inicie o backend redirecionando a saída e use `grep`:

```bash
cd backend && ./mvnw spring-boot:run | tee ../backend.log
```

```bash
grep "RASTREIO-001" backend.log
```

Se você não mandar o header, o backend gera um UUID e o devolve na resposta — o efeito é o
mesmo, só que o id é sorteado.

Para ver apenas as ações de negócio, sem o ruído de SQL:

```bash
grep "acao=" backend.log
```

### Métricas de negócio

```bash
curl -s http://localhost:8080/actuator/prometheus | grep "^helpdesk_"
```

| Métrica | Tipo | Tags |
|---|---|---|
| `helpdesk_chamados_abertos_total` | Counter | `categoria`, `prioridade` |
| `helpdesk_chamados_transicoes_total` | Counter | `de`, `para`, `perfil` |
| `helpdesk_chamados_por_status` | Gauge | `status` |
| `helpdesk_chamados_fora_sla` | Gauge | — |
| `helpdesk_tempo_resolucao_segundos` | Timer | `categoria` |

> Os dois **gauges** são recalculados a cada leitura de `GET /api/dashboard/resumo`. Se
> você nunca abriu o dashboard, eles valem zero — abra a tela e faça o scrape de novo.

---

## 8. A API

Base: `/api`. Listagens de chamados aceitam `page`, `size` e `sort`, e devolvem o envelope
padrão do Spring Data (`content`, `totalElements`, `totalPages`, `number`).

| Método | Rota | Perfil |
|---|---|---|
| GET | `/auth/usuarios-disponiveis` | Público |
| POST | `/auth/login` | Público |
| GET | `/chamados` | Todos (solicitante vê só os seus) |
| GET | `/chamados/{id}` | Todos (solicitante só os seus) |
| POST | `/chamados` | Todos |
| PATCH | `/chamados/{id}/assumir` | Atendente, Admin |
| PATCH | `/chamados/{id}/status` | Conforme a tabela de permissões |
| GET | `/chamados/{id}/comentarios` | Todos com acesso ao chamado |
| POST | `/chamados/{id}/comentarios` | Todos com acesso ao chamado |
| GET | `/categorias` | Todos (`?incluirInativas=true` só Admin) |
| POST · PUT · DELETE | `/categorias`, `/categorias/{id}` | Admin |
| GET · POST · PUT · DELETE | `/usuarios`, `/usuarios/{id}` | Admin |
| GET | `/dashboard/resumo` | Atendente, Admin |

`DELETE` é sempre **soft delete**: desativa o registro, nunca o remove.

### Padrão de erro

```json
{
  "timestamp": "2026-09-21T22:10:00Z",
  "status": 422,
  "erro": "TRANSICAO_INVALIDA",
  "mensagem": "Chamado ABERTO nao pode ir direto para RESOLVIDO"
}
```

| HTTP | `erro` | Quando |
|---|---|---|
| 400 | `VALIDACAO` | Campo inválido (o corpo ganha a lista `campos`), JSON malformado |
| 401 | `NAO_AUTENTICADO` | `X-User-Id` ausente, inválido, inexistente ou de usuário inativo |
| 403 | `ACESSO_NEGADO` | Perfil ou relação com o chamado não permitem a ação |
| 404 | `RECURSO_NAO_ENCONTRADO` | Id inexistente |
| 422 | `TRANSICAO_INVALIDA` | Transição fora da máquina de estados |
| 422 | `REGRA_NEGOCIO` | Outra regra violada (nome duplicado, categoria inativa...) |
| 500 | `ERRO_INTERNO` | Exceção não prevista. Nunca vaza stack trace |

---

## 9. Testes

### Backend — 151 testes

| Nível | Arquivo | Cobre |
|---|---|---|
| Unitário — domínio | `TransicaoStatusTest` | As 48 combinações de status × perfil; exatamente 8 são válidas |
| Unitário — domínio | `SlaCalculatorTest` | Prazo, fora do SLA (aberto vs. resolvido tarde), tempo médio com 0, 1 e vários |
| Unitário — service | `PermissaoServiceTest` | Cada linha da tabela de permissões, incluindo os casos "só os seus" |
| Unitário — service | `ChamadoServiceTest` | Assumir, carimbos com `Clock` fixo e **permissão antes da transição** |
| Unitário — service | `CategoriaServiceTest`, `UsuarioServiceTest`, `DashboardServiceTest`, `MetricasNegocioTest` | CRUD, soft delete, agregações e métricas |
| Slice — web | `ChamadoControllerTest` (`@WebMvcTest`) | 400/401/403/404/422 e o formato do JSON de erro |
| Slice — dados | `ChamadoSpecificationTest` (`@DataJpaTest`) | Solicitante não vê chamados alheios **nem no `count`**; filtros combinados |
| Integração | `CicloDeVidaChamadoIT` (`@SpringBootTest`) | Abrir → assumir → comentar → resolver → fechar, trocando `X-User-Id` |

Fixtures vêm dos builders em `backend/src/test/java/br/com/helpdesk/support/` — **nenhum
teste depende do `data.sql`**, que é fixture de demonstração e muda quando a demo muda.

### Frontend — 49 testes

`authStore` (helpers de perfil, sessão, `localStorage`), `chamadosStore` (filtros,
paginação, recarga após ação — com **Axios mockado**), `ChamadoAcoes` (botões visíveis
por perfil × status) e `SeletorTema` (os três modos, persistência e o `color-scheme`).

---

## 10. Limitações conhecidas do MVP

Cortes de escopo deliberados, documentados na seção 9 de `docs/ARQUITETURA.md`:

| Limitação | Por quê | Evolução |
|---|---|---|
| **Sem segurança real** — `X-User-Id` é confiável por definição | O foco da atividade é o domínio | Spring Security + JWT. Só o `UsuarioAtualFilter` muda |
| Dados somem a cada restart | H2 em memória, zero instalação | PostgreSQL/SQL Server |
| Sem anexos e sem notificações | Fora do escopo | Storage de arquivos; e-mail/websocket |
| Sem testes E2E | Fora do escopo do MVP | Cypress |
| Auditoria só em log | Sem tabela extra no MVP | Tabela `chamado_evento` |

> **Não exponha este sistema fora de `localhost`.** Qualquer pessoa que consiga mandar um
> header `X-User-Id` vira o usuário que quiser.

---

## 11. Sobre a construção

O projeto foi implementado por um agente de IA (Claude Code) a partir de três artefatos, na
ordem definida pela disciplina:

1. o documento de arquitetura da Aula 1 —
   [Atividade 1/01_Arquitetura_HelpDesk_Lite v3.md](../Atividade%201/01_Arquitetura_HelpDesk_Lite%20v3.md);
2. o prompt de geração de contexto, que produziu a pasta `.ai/` —
   [Atividade 2/02_Prompt_Geracao_Contexto v3.md](../Atividade%202/02_Prompt_Geracao_Contexto%20v3.md);
3. o prompt de implementação, executado em quatro etapas —
   [Atividade 2/03_Prompt_Implementacao v3.md](../Atividade%202/03_Prompt_Implementacao%20v3.md).

Nove ambiguidades do documento original foram identificadas e resolvidas durante a geração
do contexto — cada uma está registrada como ADR na seção 2 de
[.ai/architecture.md](../.ai/architecture.md), com a lista completa na seção 8 do mesmo
arquivo. As principais:

- **ADR-015** — a fronteira entre 403 e 422 na alteração de status.
- **ADR-016** — reabertura é ação do solicitante, não do atendente.
- **ADR-017** — 401 existe (o documento não o listava) e é escrito pelo filter.
- **ADR-018** — os gauges são recalculados na leitura do dashboard.
