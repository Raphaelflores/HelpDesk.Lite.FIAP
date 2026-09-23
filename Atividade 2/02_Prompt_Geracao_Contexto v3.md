# Prompt de Geração de Contexto — HelpDesk Lite

**Disciplina:** AI Driven Development — Aula 2 (Prática: Implementando a sua arquitetura)
**Ferramenta usada:** Claude Code _(ou Antigravity — o prompt é agnóstico)_
**Pré-requisito:** repositório com `docs/ARQUITETURA.md` na raiz (o documento da Aula 1).

## Como usar

1. Crie o repositório `helpdesk-lite` e coloque o documento da Aula 1 em `docs/ARQUITETURA.md`.
2. Abra o agente na raiz do repositório.
3. Cole o prompt abaixo. O resultado esperado é a pasta `.ai/` com os quatro arquivos preenchidos.
4. Revise os arquivos gerados antes de rodar o prompt de implementação — eles são a "constituição" do projeto.

---

## Prompt

```
Você é um arquiteto de software sênior preparando o contexto de um projeto para ser
implementado por agentes de IA.

## Fonte da verdade
Leia integralmente o arquivo `docs/ARQUITETURA.md`. Tudo o que você gerar deve ser
derivado dele. Não invente funcionalidades, entidades, endpoints ou tecnologias que não
estejam lá. Se algo estiver ambíguo, escolha a interpretação mais simples e registre a
decisão em `architecture.md` como um ADR.

## Objetivo
Criar a pasta `.ai/` na raiz do repositório com exatamente esta estrutura:

.ai/
├── standards.md        # Convenções de código e estilo
├── architecture.md     # Decisões de alto nível (ADRs)
├── tech-stack.md       # Versões e libs permitidas
└── business-rules.md   # Lógica de negócio e domínio

## Conteúdo esperado de cada arquivo

### standards.md
- Estrutura de pastas do monorepo e de pacotes do backend exatamente como na seção 4.8
  do documento (`config`, `web/{filter,controller,dto,exception}`, `service`,
  `domain/{model,enums,state,exception}`, `repository`).
- Convenções de nomenclatura: classes em PascalCase, métodos e variáveis em camelCase,
  tabelas e colunas em snake_case, enums em UPPER_SNAKE_CASE, rotas REST em kebab-case
  no plural.
- Regras de código: DTOs para entrada e saída (nunca expor entidade JPA diretamente),
  validação com Bean Validation nos DTOs de entrada, tratamento de erro centralizado com
  `@RestControllerAdvice` seguindo o padrão de erro definido no documento.
- Frontend: componentes em PascalCase, um store Pinia por domínio, chamadas à API
  centralizadas em `src/services/`, nenhuma URL hardcoded fora de `src/services/api.js`.
- Testabilidade (seção 4.12 do documento): regras de domínio em classes puras sem
  dependência de Spring; `java.time.Clock` injetado em vez de `Instant.now()`;
  `UsuarioAtual` chega por parâmetro no service, nunca via `ThreadLocal` ou contexto
  estático; repositórios só via interface. Convenções de teste: nome descreve a regra
  (`deveRejeitarTransicaoDeAbertoParaResolvido`), padrão given/when/then, fixtures via
  builders em `src/test/.../support/`, nunca reaproveitando o `data.sql`. Estrutura de
  pastas de teste conforme a árvore da seção 4.8.
- Observabilidade (seção 4.11): todo log de negócio via SLF4J, nunca `System.out`;
  transições de status e ações administrativas geram uma linha `INFO` estruturada com
  `requestId`, `usuarioId`, `perfil`, `acao`, `chamadoId`, `statusDe`, `statusPara`;
  4xx logados em `WARN` sem stack trace, 5xx em `ERROR` com stack trace; nenhum dado
  pessoal além do id do usuário em `INFO` ou acima; métricas só através da classe
  `MetricasNegocio`, nunca `MeterRegistry` direto no service.
- Regras para commits: mensagens no formato `tipo: descrição` (feat, fix, chore, docs, test).
- Idioma: código em inglês, textos de UI e mensagens de erro em português do Brasil.

### architecture.md
- Resumo da arquitetura em três camadas e responsabilidade de cada uma.
- Lista de ADRs no formato: contexto, decisão, alternativas, consequências. Copie
  os 13 ADRs da seção 4.7 do documento mantendo a numeração, e acrescente
  ADR-014 CORS liberado apenas para `http://localhost:9000` no perfil dev.
- Regra de dependência entre camadas (`web → service → domain ← repository`) e o que
  cada camada NÃO pode fazer, conforme a tabela de componentes da seção 4.4.
- Ordem obrigatória de verificação em operações sobre chamado: permissão (403) antes
  de transição (422), conforme seção 4.6.
- Observabilidade (seção 4.11): `RequestIdFilter` como primeiro filtro da cadeia,
  Actuator com quais endpoints abertos em `dev` e em `prod`, a tabela de métricas de
  negócio (nome, tipo, tags, onde é registrada) copiada fielmente, e o que fica para a
  evolução (Prometheus/Grafana, OpenTelemetry).
- Testabilidade (seção 4.12): a pirâmide de testes adotada (unitário → slice →
  integração; E2E fora do MVP), a tabela de casos obrigatórios por nível copiada
  fielmente, e a meta de cobertura (80% em `domain` e `service` via JaCoCo).
- Mapa de fluxos principais referenciando as seções do documento.

### tech-stack.md
- Backend: Java 21, Spring Boot 3.3.x, Spring Web, Spring Data JPA, Bean Validation,
  H2, springdoc-openapi 2.x, Lombok, Spring Boot Actuator, Micrometer (registry
  Prometheus). Maven como build.
- Testes backend: JUnit 5, Mockito, AssertJ, Spring Boot Test (`@WebMvcTest`,
  `@DataJpaTest`, `@SpringBootTest` + `MockMvc`), JaCoCo com verificação de 80% em
  `domain` e `service` no `mvn verify`.
- Frontend: Node 20 LTS, Vue 3.4+, Quasar 2.x (via Quasar CLI com Vite), Pinia, Axios,
  Vue Router.
- Testes frontend: Vitest + @vue/test-utils.
- Fixe versões explícitas. Liste o que é PROIBIDO adicionar sem aprovação: qualquer
  lib de segurança (Spring Security), qualquer banco além do H2, qualquer lib de UI além
  do Quasar, qualquer ferramenta de state management além do Pinia.
- Comandos oficiais: `mvn spring-boot:run` (porta 8080), `quasar dev` (porta 9000),
  `mvn test`, `mvn verify` (testes + cobertura), `npm run test` (Vitest), `quasar build`.
- URLs de operação: `/swagger-ui.html`, `/h2`, `/actuator/health`, `/actuator/metrics`,
  `/actuator/prometheus`.

### business-rules.md
- Enums e seus valores: `Perfil`, `StatusChamado`, `Prioridade`.
- Tabela de permissões ação x perfil, copiada fielmente do documento.
- Transições de status permitidas e quem pode executar cada uma. Qualquer outra
  transição retorna 422 com erro `TRANSICAO_INVALIDA`.
- Regras de integridade: solicitante e categoria obrigatórios; atendente só é preenchido
  ao assumir; comentário imutável; categoria com chamados não pode ser excluída.
- Regras de SLA e fórmulas do dashboard exatamente como descritas no documento.
- Regras de visibilidade: solicitante só enxerga os próprios chamados em qualquer
  endpoint de leitura.
- Dados de seed obrigatórios: 3 usuários (um de cada perfil), 4 categorias com SLAs
  diferentes, 8 chamados distribuídos entre todos os status e prioridades (incluindo
  pelo menos 1 fora do SLA), 6 comentários.

## Restrições
- Não crie nenhum arquivo além dos quatro dentro de `.ai/`.
- Não escreva código de aplicação nesta etapa.
- Cada arquivo deve ser autocontido e legível por um agente sem acesso a este prompt.
- Ao terminar, liste os quatro arquivos criados e aponte qualquer ponto do documento
  original que você considerou ambíguo e como resolveu.
```
