# Prompt de Implementação — HelpDesk Lite

**Disciplina:** AI Driven Development — Aula 2 (Prática: Implementando a sua arquitetura)
**Ferramenta usada:** Claude Code _(ou Antigravity)_
**Pré-requisito:** pasta `.ai/` gerada pelo prompt de contexto e revisada.

## Como usar

1. Na raiz do repositório, com `.ai/` e `docs/ARQUITETURA.md` presentes, cole o prompt abaixo.
2. Deixe o agente executar etapa por etapa. Se ele parar pedindo confirmação, revise o que foi feito antes de liberar.
3. Ao final, siga o `README.md` gerado para subir backend e frontend e gravar o vídeo.

---

## Prompt

```
Você é um agente de desenvolvimento full-stack. Vai implementar o MVP do HelpDesk Lite.

## Antes de escrever qualquer código
1. Leia TODOS os arquivos em `.ai/` (standards.md, architecture.md, tech-stack.md,
   business-rules.md) e `docs/ARQUITETURA.md`.
2. Eles são a fonte da verdade. Em caso de conflito entre eles, `.ai/` prevalece sobre
   `docs/`. Não adicione dependências, features ou tecnologias fora do que está definido.
3. Resuma em 10 linhas o que vai construir e confirme que entendeu as regras de negócio
   antes de começar.

## Objetivo
MVP funcional, rodando localmente, com dados mockados, cobrindo o fluxo completo:
abrir chamado → assumir → comentar → resolver → fechar, mais dashboard e CRUD de
categorias/usuários para o Admin.

## Etapas — execute em ordem e faça um commit ao final de cada uma

### Etapa 1 — Backend: base e domínio
- Gere o projeto Spring Boot em `backend/` conforme `tech-stack.md`.
- Crie entidades JPA, enums e repositories conforme `business-rules.md` e o ER do documento.
- Configure H2 em memória, console H2 habilitado em `/h2`, springdoc em `/swagger-ui.html`,
  Actuator com `health`, `metrics` e `prometheus` expostos no perfil `dev`.
- Configure `ClockConfig` (bean `Clock.systemUTC()`) e `logback-spring.xml` com
  `requestId` e `usuarioId` do MDC no pattern.
- Crie `data.sql` com o seed obrigatório descrito em `business-rules.md`.
- Critério de aceite: `mvn spring-boot:run` sobe sem erro e o console H2 mostra os dados.

### Etapa 2 — Backend: regras e API
- Implemente services com todas as regras de negócio: transições de status, permissões
  por perfil, visibilidade do solicitante, soft delete, cálculo de SLA.
- Implemente todos os endpoints da seção 6 do documento, com DTOs e Bean Validation.
- Implemente `RequestIdFilter` (primeiro da cadeia: gera/propaga `X-Request-Id`, MDC,
  devolve no header) e `UsuarioAtualFilter` (lê `X-User-Id`, disponibiliza o usuário
  atual, coloca `usuarioId` no MDC).
- Implemente `MetricasNegocio` com as métricas da tabela em `architecture.md` e o log
  de auditoria estruturado em toda transição de status e ação administrativa.
- Implemente o `@RestControllerAdvice` com o padrão de erro definido.
- Escreva os testes obrigatórios da tabela "casos por nível" em `architecture.md`:
  unitários de `TransicaoStatus`, `SlaCalculator` (com `Clock.fixed`), `PermissaoService`
  e `ChamadoService` (repos mockados); `@WebMvcTest` do `ChamadoController` cobrindo
  400/401/403/404/422; `@DataJpaTest` da `ChamadoSpecification`; e um `@SpringBootTest`
  com o ciclo de vida completo trocando `X-User-Id`. Use os builders de `support/`.
- Critério de aceite: `mvn verify` passa com cobertura ≥ 80% em `domain` e `service`;
  via Swagger consigo executar o fluxo completo trocando o `X-User-Id`;
  `/actuator/health` responde `UP`; `/actuator/prometheus` lista as métricas `helpdesk_*`;
  cada resposta traz `X-Request-Id` e o mesmo id aparece nas linhas de log da requisição.

### Etapa 3 — Frontend
- Gere o projeto Quasar em `frontend/` conforme `tech-stack.md`.
- Telas obrigatórias:
  - Login simulado: lista usuários de `/auth/usuarios-disponiveis`, escolhe um, guarda no Pinia.
  - Lista de chamados: tabela com filtros (status, prioridade, categoria, "meus"), badge
    colorido por status e prioridade, ações visíveis conforme perfil.
  - Detalhe do chamado: dados, linha do tempo de comentários, botões de ação
    (Assumir / Resolver / Fechar / Reabrir) habilitados conforme regras.
  - Novo chamado: formulário com validação.
  - Categorias e Usuários (Admin): CRUD simples em tabela + dialog.
  - Dashboard (Atendente/Admin): cards com totais e lista de chamados fora do SLA.
- Layout com menu lateral que esconde itens sem permissão para o perfil logado.
- Toda chamada à API passa por `src/services/api.js`, que injeta `X-User-Id` e um
  `X-Request-Id` gerado por requisição, e trata erros mostrando `q-notify` com a mensagem
  retornada pelo backend e o request id (pra correlacionar com o log).
- Testes Vitest de `authStore`, `chamadosStore` (Axios mockado) e do componente
  `ChamadoAcoes` (botões conforme perfil + status).
- Critério de aceite: `quasar dev` sobe; `npm run test` passa; consigo fazer o fluxo
  completo pela interface logando como cada um dos três perfis.

### Etapa 4 — Documentação e entrega
- Crie `README.md` na raiz com: descrição, stack, pré-requisitos, como rodar backend e
  frontend, usuários de teste do seed, link do Swagger, roteiro sugerido de demonstração
  (passo a passo pra gravar um vídeo de 3 a 5 minutos).
- Crie `.gitignore` adequado para Java/Maven e Node.
- Inclua no README uma seção "Operação" com as URLs de Swagger, H2, Actuator e um
  exemplo de como achar no log tudo que aconteceu numa requisição pelo `X-Request-Id`.
- Garanta que `mvn verify`, `npm run test` e `quasar build` passam do zero em um clone limpo.

## Regras durante a execução
- Não pule etapas nem antecipe features de etapas futuras.
- Não altere nada em `.ai/` ou `docs/`. Se achar que uma regra está errada, pare e reporte.
- Prefira soluções simples e diretas. Sem abstrações "pra escalar depois".
- Ao encontrar erro de build ou teste, corrija e rode de novo antes de seguir.
- Ao final de cada etapa, escreva um resumo curto: o que foi feito, comandos para validar,
  problemas encontrados e como resolveu. Esses resumos vão virar o roteiro do vídeo.

## Entrega final esperada
helpdesk-lite/
├── .ai/
├── docs/ARQUITETURA.md
├── backend/         # Spring Boot rodando em :8080
├── frontend/        # Quasar rodando em :9000
├── .gitignore
└── README.md
```
