# Arquitetura — HelpDesk Lite

Decisões de alto nível do projeto. Derivado da seção 4 de `docs/ARQUITETURA.md`.

---

## 1. Resumo

**Monolito modular em camadas**, com dependência unidirecional
`web -> service -> domain <- repository`. Frontend e backend são processos separados,
acoplados só por um contrato REST/JSON.

| Camada | Responsabilidade | O que **não** faz |
|---|---|---|
| **web** | Rotas, status HTTP, DTOs de entrada/saída, Bean Validation, filtros de request id e usuário atual, tradução de exceção em JSON | Nenhuma regra de negócio. Não acessa repositório |
| **service** | Orquestra o caso de uso: busca -> permissão -> transição -> persiste -> mapeia. Permissão, SLA, métricas e mapeamento | Não formata resposta HTTP. Não conhece `HttpServletRequest` nem `ResponseEntity` |
| **domain** | Entidades, enums, máquina de estados, exceções de negócio. Regras puras | Não conhece Spring, HTTP nem repositórios |
| **repository** | Interfaces Spring Data, `Specification` de filtros e visibilidade, seed | Nenhuma regra de negócio |

O domínio não conhece a camada web nem a de persistência. As entidades carregam anotações
JPA, mas nenhuma regra de negócio depende do Spring.

---

## 2. ADRs

Os ADRs 001 a 013 são cópia da seção 4.7 do documento de arquitetura, com a numeração
preservada. Os ADRs 014 a 018 foram acrescentados ao gerar este contexto: 014 por
instrução explícita, 015 a 018 resolvendo ambiguidades do documento original (registradas
na seção 8).

---

### ADR-001 — Monorepo com `backend/` e `frontend/`

- **Contexto.** O sistema tem duas aplicações executáveis que evoluem juntas e são
  entregues como um trabalho único.
- **Decisão.** Um repositório com as duas pastas na raiz.
- **Alternativas.** Dois repositórios separados.
- **Consequências.** Uma entrega, um clone, um vídeo. Separar só faria sentido com times
  ou pipelines distintos, que não existem aqui.

### ADR-002 — Monolito em camadas

- **Contexto.** 4 entidades e cerca de 20 endpoints.
- **Decisão.** Monolito em camadas com dependência unidirecional.
- **Alternativas.** Arquitetura hexagonal / clean; microsserviços.
- **Consequências.** Portas e adaptadores não se pagam nesse tamanho. A dependência
  unidirecional já dá o isolamento necessário. Migrar para hexagonal depois é um
  refactor local, não uma reescrita.

### ADR-003 — Autenticação simulada via header `X-User-Id` resolvido em um filter

- **Contexto.** O MVP precisa de perfis distintos, mas segurança real está fora do escopo.
- **Decisão.** O cliente envia `X-User-Id`; um `UsuarioAtualFilter` resolve o usuário e
  responde 401 se o header estiver ausente, for inválido ou o usuário estiver inativo.
- **Alternativas.** Spring Security + JWT desde o início.
- **Consequências.** Foco no domínio. O filter é o único ponto que conhece o mecanismo —
  trocar por JWT mexe em uma classe. **O sistema não tem segurança real e não deve ser
  exposto fora de `localhost`.**

### ADR-004 — Autorização em `PermissaoService` chamado pelo service

- **Contexto.** As regras de acesso dependem da **relação** entre usuário e chamado
  (é o solicitante? é o atendente responsável?), não só do perfil.
- **Decisão.** Uma classe `PermissaoService`, invocada explicitamente pelo service de
  aplicação, traduz a tabela de permissões em código.
- **Alternativas.** `@PreAuthorize` por método.
- **Consequências.** Sem Spring Security no MVP. Isso é regra de negócio, não de
  infraestrutura, e fica testável com `new` + JUnit. O custo é ter de lembrar de chamar a
  verificação — mitigado por teste de service que exige permissão antes da transição.

### ADR-005 — Máquina de estados em `TransicaoStatus`

- **Contexto.** O chamado tem 4 estados e 4 transições, cada uma executável por perfis
  distintos.
- **Decisão.** Uma tabela `de -> para -> perfis` em uma classe de domínio.
- **Alternativas.** `if/else` no service; Spring StateMachine.
- **Consequências.** Um lugar só, testável isoladamente, legível como tabela. Spring
  StateMachine é exagero para 4 arestas.

### ADR-006 — DTOs com mapeamento manual

- **Contexto.** Poucas classes de DTO.
- **Decisão.** Mapeamento escrito à mão em classes de `service/mapper/`.
- **Alternativas.** MapStruct; expor a entidade JPA direto.
- **Consequências.** Evita mais uma lib e um processador de anotações. Expor entidade
  acoplaria a API ao banco e causaria lazy-loading acidental na serialização.

### ADR-007 — Filtros com JPA `Specification`

- **Contexto.** Quatro filtros opcionais combináveis mais a regra de visibilidade.
- **Decisão.** `ChamadoSpecification` compõe os predicados dinamicamente.
- **Alternativas.** Métodos derivados (`findByStatusAndPrioridade...`); JPQL manual.
- **Consequências.** Métodos derivados explodiriam em combinações. `Specification` compõe
  e garante que a visibilidade entre no `WHERE` — logo, também no `count` da paginação.

### ADR-008 — H2 em memória com `data.sql`

- **Contexto.** O MVP precisa rodar em qualquer máquina, sem instalação.
- **Decisão.** H2 em memória, seed via `data.sql`.
- **Alternativas.** PostgreSQL via Docker.
- **Consequências.** Zero setup. Os dados somem a cada restart, o que é aceitável (e até
  desejável) numa demonstração. JPA abstrai a troca; um `application-prod.yml` aponta
  para outro datasource sem mexer em código.

### ADR-009 — Soft delete (`ativo = false`) em usuário e categoria

- **Contexto.** Chamados históricos referenciam quem os abriu e sua categoria.
- **Decisão.** `DELETE` desativa o registro em vez de removê-lo.
- **Alternativas.** Hard delete com cascata.
- **Consequências.** O histórico continua íntegro. Em troca, toda listagem de seleção
  precisa filtrar por `ativo = true`.

### ADR-010 — Erros como exceções de domínio + handler central

- **Contexto.** Cinco famílias de erro precisam virar cinco status HTTP.
- **Decisão.** Exceções de domínio capturadas por um `@RestControllerAdvice`.
- **Alternativas.** Retornar um objeto `Result`/`Either` de cada service.
- **Consequências.** Idiomático em Spring, menos ruído no service, mapeamento HTTP
  concentrado em uma classe.

### ADR-011 — Listagens paginadas desde o MVP

- **Contexto.** A lista de chamados cresce indefinidamente.
- **Decisão.** `Pageable` do Spring Data em todas as listagens de chamados
  (`page`, `size`, `sort`; padrão `size=20`, `sort=criadoEm,desc`).
- **Alternativas.** Retornar a lista completa.
- **Consequências.** Custo praticamente zero com Spring Data, e evita reescrever a tela
  depois. Categorias e usuários, que são listas curtas de cadastro, continuam sem
  paginação.

### ADR-012 — Observabilidade via Actuator + Micrometer + `X-Request-Id` no MDC

- **Contexto.** É preciso responder "está saudável?", "o que aconteceu no chamado #123?"
  e "quantos estão fora do SLA?" sem abrir o banco.
- **Decisão.** Actuator + Micrometer + `RequestIdFilter` alimentando o MDC do Logback.
- **Alternativas.** Só logs soltos; ou já subir Prometheus/Grafana/OTel no MVP.
- **Consequências.** Uma dependência e quase nenhum código dão health, métricas técnicas
  e correlação de logs. A stack externa pluga depois sem tocar no negócio.

### ADR-013 — `Clock` injetável e `UsuarioAtual` por parâmetro

- **Contexto.** SLA e carimbos de tempo são regra de negócio; permissão depende do usuário.
- **Decisão.** Um bean `Clock.systemUTC()` injetado onde há tempo, e `UsuarioAtual`
  passado como parâmetro para o service.
- **Alternativas.** `Instant.now()` direto; `ThreadLocal` / `SecurityContextHolder`.
- **Consequências.** Regras de SLA e permissão viram testes unitários puros, com
  `Clock.fixed(...)` e sem mock de contexto estático.

### ADR-014 — CORS liberado apenas para `http://localhost:9000` no perfil `dev`

- **Contexto.** No MVP a SPA (porta 9000) e a API (porta 8080) são origens diferentes,
  então o navegador exige CORS.
- **Decisão.** `CorsConfig` libera **exatamente** `http://localhost:9000`, com os métodos
  `GET, POST, PUT, PATCH, DELETE, OPTIONS`, todos os headers, e expõe `X-Request-Id` na
  resposta. Sem `allowedOrigins("*")`.
- **Alternativas.** Liberar `*`; usar proxy do Vite para `/api`.
- **Consequências.** A origem única deixa explícito que isso é configuração de
  desenvolvimento. `X-Request-Id` precisa estar em `exposedHeaders`, senão o JavaScript
  não consegue lê-lo e o `q-notify` perde a correlação com o log. Em produção a SPA e a
  API ficam sob a mesma origem via Nginx e o CORS deixa de existir.

### ADR-015 — Fronteira entre 403 e 422 na alteração de status

- **Contexto.** O documento exige verificar permissão (403) **antes** da transição (422),
  mas a tabela da máquina de estados também lista perfis por aresta. Sem um critério, a
  mesma regra caberia nos dois lugares.
- **Decisão.** Dois papéis distintos e complementares:
  - `PermissaoService` responde **"quem pode"** — perfil + relação + status-alvo,
    **ignorando o status atual**. Falhou -> 403.
  - `TransicaoStatus` responde **"pode agora"** — a aresta `(de, para)` existe e esse
    perfil a executa. Falhou -> 422.
- **Alternativas.** Colocar tudo em `TransicaoStatus` (422 para tudo, perdendo o 403) ou
  tudo em `PermissaoService` (que precisaria conhecer o status atual, duplicando a
  máquina de estados).
- **Consequências.** Um atendente que tenta resolver o chamado de outro recebe **403** e
  não descobre em que estado aquele chamado está — que é exatamente o vazamento que a
  ordem fixa de verificação quer evitar. Em compensação, o mesmo perfil pode receber 403
  ou 422 para a mesma rota conforme a relação com o chamado; a tabela de exemplos da
  seção 3.3 de `business-rules.md` existe para tornar isso previsível.

### ADR-016 — Reabertura é ação do solicitante

- **Contexto.** O diagrama de estados marca `RESOLVIDO -> EM_ATENDIMENTO` como
  "solicitante reabre", mas a tabela de permissões da seção 3 não tem uma linha
  "reabrir chamado".
- **Decisão.** A aresta de reabertura é executável por `SOLICITANTE` (dono do chamado) e
  `ADMIN`, pela mesma rota `PATCH /chamados/{id}/status`. O atendente não reabre.
- **Alternativas.** Criar um endpoint `/reabrir` dedicado; permitir que o atendente
  reabra.
- **Consequências.** Mantém o diagrama de estados como está desenhado e não inventa
  endpoint fora da seção 6. Reabrir limpa `resolvidoEm` e `fechadoEm`, mas **preserva o
  atendente**, para que o chamado volte para a fila de quem já o conhece.

### ADR-017 — 401 escrito pelo filter, fora do `@RestControllerAdvice`

- **Contexto.** A lista de códigos HTTP da seção 6 do documento não cita 401, mas a
  tabela de componentes exige 401 no `UsuarioAtualFilter` e a tabela de testes exige
  cobrir "401 sem header".
- **Decisão.** 401 existe e é `NAO_AUTENTICADO`. Como o filter roda antes do
  `DispatcherServlet`, ele serializa o `ErroResponse` direto na resposta, em vez de lançar
  exceção.
- **Alternativas.** Um `HandlerExceptionResolver` no filter; deixar o 401 virar 500.
- **Consequências.** O corpo do 401 tem o mesmo formato de todos os outros erros, que é o
  que o `api.js` do frontend espera. O custo é o filter precisar de um `ObjectMapper` —
  aceitável para uma única classe.

### ADR-018 — Gauges recalculados na leitura do dashboard

- **Contexto.** `helpdesk_chamados_por_status` e `helpdesk_chamados_fora_sla` são gauges,
  e o documento diz "atualizada a cada leitura".
- **Decisão.** `DashboardService.resumo()` recalcula os valores e os publica em
  `MetricasNegocio`, que mantém os holders (`AtomicLong`) registrados no `MeterRegistry`.
  Todos os valores de status são pré-registrados no startup, valendo zero.
- **Alternativas.** Um `@Scheduled` periódico; `Gauge` com função que consulta o banco a
  cada scrape.
- **Consequências.** Nenhum agendador e nenhuma query disparada pelo Prometheus. Em troca,
  o gauge fica desatualizado se ninguém abrir o dashboard — aceitável no MVP, e o
  `/actuator/prometheus` mostra zero até a primeira leitura. A evolução natural é o
  `Gauge` com função de consulta quando houver scrape de verdade.

---

## 3. Regra de dependência entre camadas

```
web  ->  service  ->  domain  <-  repository
```

| Componente | Responsabilidade | **Não faz** |
|---|---|---|
| `RequestIdFilter` | Gera ou reaproveita `X-Request-Id`, coloca no MDC, devolve no header da resposta | Não sabe quem é o usuário |
| `UsuarioAtualFilter` | Lê `X-User-Id`, carrega o usuário, publica `UsuarioAtual` no request e `usuarioId` no MDC; 401 se ausente, inválido ou inativo | Não decide permissão |
| Controllers | Rotas, status HTTP, DTO de entrada/saída, delega para o service | Nenhuma regra de negócio |
| `PermissaoService` | Traduz a tabela de permissões em código: perfil + relação com o chamado | Não acessa banco |
| `TransicaoStatus` | Tabela de transições válidas e quem executa cada uma. Único lugar que conhece a máquina de estados | Não sabe de HTTP |
| `ChamadoService` | Orquestra: busca -> permissão -> transição -> persiste -> mapeia | Não formata resposta HTTP |
| `ChamadoSpecification` | Monta a query dinâmica dos filtros e **injeta `solicitanteId = usuarioAtual` quando o perfil é `SOLICITANTE`** | — |
| `SlaCalculator` | Prazo, fora do SLA, tempo médio de resolução. Recebe `Clock` injetado | Não acessa banco |
| `MetricasNegocio` | Encapsula counters, gauges e timers. Único ponto que fala com o `MeterRegistry` | Não contém regra de negócio |
| `GlobalExceptionHandler` | Converte exceção em JSON padrão | — |

---

## 4. Ordem obrigatória de verificação

Em toda operação sobre um chamado:

1. **Existe?** -> `RecursoNaoEncontradoException` -> **404**
2. **Quem pode?** — `PermissaoService` -> `AcessoNegadoException` -> **403**
3. **Pode agora?** — `TransicaoStatus` -> `TransicaoInvalidaException` -> **422**

Primeiro *quem pode*, depois *se pode agora*. Inverter vazaria o estado do chamado para
quem não deveria vê-lo. Há um teste de `ChamadoService` que existe só para travar essa
ordem (`deveVerificarPermissaoAntesDaTransicao`).

---

## 5. Observabilidade

### 5.1 Cadeia de filtros

`RequestIdFilter` é o **primeiro** filtro da cadeia (`@Order(1)`), antes do
`UsuarioAtualFilter` (`@Order(2)`). Motivo: uma requisição rejeitada com 401 também
precisa aparecer no log correlacionada por `requestId`.

| Filtro | Ordem | O que põe no MDC |
|---|---|---|
| `RequestIdFilter` | 1 | `requestId` |
| `UsuarioAtualFilter` | 2 | `usuarioId` |

Ambos limpam o MDC no `finally`. O `X-Request-Id` volta no header da resposta e é exibido
no `q-notify` de erro do frontend.

### 5.2 Actuator

| Perfil | Endpoints expostos |
|---|---|
| `dev` | `health` (com `show-details: always`), `metrics`, `prometheus`, `info`, `env`, `loggers` — todos em `localhost` |
| `prod` | Somente `health` e `prometheus`, atrás da rede interna |

### 5.3 Métricas de negócio

| Métrica | Tipo | Tags | Onde é registrada |
|---|---|---|---|
| `helpdesk_chamados_abertos_total` | Counter | `categoria`, `prioridade` | `ChamadoService.abrir` |
| `helpdesk_chamados_transicoes_total` | Counter | `de`, `para`, `perfil` | `ChamadoService.alterarStatus` / `assumir` |
| `helpdesk_chamados_por_status` | Gauge | `status` | Consulta agregada, atualizada a cada leitura |
| `helpdesk_chamados_fora_sla` | Gauge | — | `SlaCalculator` sobre chamados abertos/em atendimento |
| `helpdesk_tempo_resolucao_segundos` | Timer / Distribution | `categoria` | Ao entrar em `RESOLVIDO` (`resolvidoEm - criadoEm`) |

Todas passam por `MetricasNegocio`. Nenhum service injeta `MeterRegistry`.

### 5.4 Log de auditoria

Toda transição de status e toda ação administrativa geram uma linha `INFO` com
`requestId`, `usuarioId`, `perfil`, `acao`, `chamadoId`, `statusDe`, `statusPara`.
Isso reconstrói a história de qualquer chamado sem tabela extra de auditoria.

Regras: nenhum dado pessoal além do id do usuário em `INFO` ou acima; `DEBUG` de SQL só
em `dev`; 4xx em `WARN` sem stack trace, 5xx em `ERROR` com stack trace.

### 5.5 Fica para a evolução

- **Prometheus + Grafana** fazendo scrape de `/actuator/prometheus` (o endpoint já existe;
  falta só o scrape e os painéis).
- **OpenTelemetry** para tracing distribuído — o `X-Request-Id` já funciona como trace id
  de fato e vira o `traceparent` sem mexer em código de negócio.
- Persistir a auditoria numa tabela `chamado_evento`, substituindo a leitura de log.

---

## 6. Testabilidade

### 6.1 Pirâmide adotada

```
        (E2E — fora do MVP, 0 testes)
     Integração   @SpringBootTest + H2 — 1 teste de ciclo de vida
   Fatias         @WebMvcTest (controller+validação+handler), @DataJpaTest (Specification)
 Unitários        domain e service com JUnit 5 + Mockito + AssertJ; Vitest no frontend
```

E2E (Cypress) está **fora do MVP**.

### 6.2 Casos obrigatórios por nível

| Nível | Alvo | Casos obrigatórios no MVP |
|---|---|---|
| Unitário — domínio | `TransicaoStatus` | Todas as transições válidas; toda transição inválida lança `TransicaoInvalidaException`; reabertura só de `RESOLVIDO` |
| Unitário — domínio | `SlaCalculator` | Prazo correto; fora do SLA aberto vs. resolvido tarde; tempo médio com zero, um e vários chamados |
| Unitário — service | `PermissaoService` | Cada linha da tabela de permissões, incluindo os casos "só os seus" |
| Unitário — service | `ChamadoService` (repos mockados) | Assumir muda atendente e status; permissão é checada **antes** da transição; `resolvidoEm`/`fechadoEm` preenchidos com o `Clock` |
| Slice — web | `@WebMvcTest(ChamadoController)` | 400 em DTO inválido; 401 sem header; 403/404/422 mapeados a partir das exceções; JSON de erro no formato padrão |
| Slice — dados | `@DataJpaTest` | `ChamadoSpecification`: solicitante não vê chamados de outros nem no `count`; combinação de filtros |
| Integração | `@SpringBootTest` + `MockMvc` | Abrir -> assumir -> comentar -> resolver -> fechar, com três usuários diferentes |
| Frontend — unitário | `authStore`, `chamadosStore`, `ChamadoAcoes` | Helpers de perfil; recarrega lista após ação; botões exibidos conforme perfil + status |

### 6.3 Meta de cobertura

**80 % de linhas em `br.com.helpdesk.domain.*` e `br.com.helpdesk.service.*`**, verificada
por JaCoCo na fase `verify` do Maven. O build **falha** abaixo disso.
Controllers, configuração e frontend não têm meta numérica no MVP.

---

## 7. Mapa de fluxos

| Fluxo | Seção do documento | Onde está no código |
|---|---|---|
| Caminho de uma requisição pelas camadas | 4.6 | `ChamadoController.alterarStatus` -> `ChamadoService.alterarStatus` |
| Ciclo de vida completo do chamado | 8.1 | `CicloDeVidaChamadoIT` |
| Login simulado | 8.2 | `AuthController` + `UsuarioAtualFilter` + `authStore` |
| Cálculo de SLA | 8.3 | `SlaCalculator` + `DashboardService` |
| Máquina de estados | 5 (stateDiagram) | `domain/state/TransicaoStatus` |
| Visibilidade do solicitante | 4.6, item 3 | `repository/ChamadoSpecification` |

---

## 8. Ambiguidades do documento original e como foram resolvidas

| # | Ambiguidade | Resolução | Onde |
|---|---|---|---|
| 1 | A ordem "permissão antes de transição" convive com uma máquina de estados que também conhece perfis — sem dizer qual das duas nega o quê | `PermissaoService` = perfil + relação + alvo (403); `TransicaoStatus` = aresta + perfil (422) | ADR-015 |
| 2 | O diagrama mostra "solicitante reabre", mas não há linha "reabrir" na tabela de permissões | Reabertura é do solicitante dono e do admin, pela rota de status | ADR-016 |
| 3 | "Códigos usados: 200, 201, 204, 400, 403, 404, 422" não inclui 401, mas o filter e a tabela de testes exigem 401 | 401 existe, com código `NAO_AUTENTICADO`, escrito pelo filter | ADR-017 |
| 4 | Gauge "atualizada a cada leitura" não diz leitura de quê | Leitura do dashboard | ADR-018 |
| 5 | O padrão de erro tem 4 campos, mas os requisitos não funcionais pedem "lista de campos inválidos" no 400 | Campo opcional `campos`, presente só no 400 de validação | `business-rules.md` §8 |
| 6 | Não é dito se comentar em chamado `FECHADO` é permitido | É permitido: não há regra que proíba, e a interpretação mais simples é não inventar uma | `business-rules.md` §2 |
| 7 | `meus=true` não é definido por perfil | Solicitante já só vê os seus; para atendente/admin significa `atendente_id = usuarioAtual` | `business-rules.md` §5 |
| 8 | Não é dito o que acontece com `resolvidoEm` ao reabrir | É zerado, junto com `fechadoEm`, para não distorcer SLA e tempo médio | `business-rules.md` §3.4 |
| 9 | ADR-011 pede paginação "nas listagens", mas categorias e usuários são cadastros curtos | Paginação só em `/chamados`; cadastros retornam lista simples | ADR-011 |
