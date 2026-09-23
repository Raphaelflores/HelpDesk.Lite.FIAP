# Padrões de Código — HelpDesk Lite

Convenções obrigatórias do projeto. Vale para todo código escrito por humanos ou agentes.

---

## 1. Estrutura de pastas

### 1.1 Monorepo

```
helpdesk-lite/
├── .ai/                    # contexto para agentes (este diretório)
├── docs/ARQUITETURA.md     # documento de arquitetura — fonte da verdade
├── backend/                # Spring Boot, porta 8080
├── frontend/               # Quasar SPA, porta 9000
├── .gitignore
└── README.md
```

### 1.2 Pacotes do backend (`br.com.helpdesk`)

```
br.com.helpdesk
├── HelpdeskApplication.java
├── config/                 # CorsConfig, OpenApiConfig, ClockConfig, WebMvcConfig
├── web/
│   ├── filter/             # RequestIdFilter, UsuarioAtualFilter, UsuarioAtual,
│   │                       # UsuarioAtualArgumentResolver
│   ├── controller/         # AuthController, ChamadoController, CategoriaController,
│   │                       # UsuarioController, DashboardController
│   ├── dto/request/        # NovoChamadoRequest, AlterarStatusRequest, ...
│   ├── dto/response/       # ChamadoResponse, ChamadoResumoResponse, ...
│   └── exception/          # GlobalExceptionHandler, ErroResponse
├── service/                # ChamadoService, CategoriaService, UsuarioService,
│   │                       # DashboardService, PermissaoService, SlaCalculator,
│   │                       # MetricasNegocio
│   └── mapper/             # ChamadoMapper, CategoriaMapper, UsuarioMapper, ComentarioMapper
├── domain/
│   ├── model/              # Usuario, Categoria, Chamado, Comentario
│   ├── enums/              # Perfil, StatusChamado, Prioridade
│   ├── state/              # TransicaoStatus
│   └── exception/          # TransicaoInvalidaException, AcessoNegadoException,
│                           # RecursoNaoEncontradoException, RegraNegocioException
└── repository/             # UsuarioRepository, CategoriaRepository, ChamadoRepository,
                            # ComentarioRepository, ChamadoSpecification
```

Recursos em `backend/src/main/resources/`: `application.yml`, `logback-spring.xml`, `data.sql`.

### 1.3 Pastas de teste do backend

Espelham a estrutura acima, uma pasta por nível da pirâmide:

```
backend/src/test/java/br/com/helpdesk/
├── domain/                 # TransicaoStatusTest, SlaCalculatorTest
├── service/                # ChamadoServiceTest, PermissaoServiceTest
├── web/                    # ChamadoControllerTest (@WebMvcTest)
├── repository/             # ChamadoSpecificationTest (@DataJpaTest)
├── integration/            # CicloDeVidaChamadoIT (@SpringBootTest)
└── support/                # builders e fixtures
```

### 1.4 Pastas do frontend

```
frontend/src/
├── boot/axios.js
├── router/                 # index.js, routes.js (meta.perfis por rota)
├── layouts/MainLayout.vue
├── pages/                  # LoginPage, ChamadosPage, ChamadoDetalhePage, ...
├── components/             # StatusBadge, PrioridadeBadge, ChamadoAcoes,
│                           # ComentarioTimeline, ChamadoFiltros
├── stores/                 # auth.js, chamados.js, catalogo.js
├── services/               # api.js, chamados.js, categorias.js, usuarios.js, dashboard.js
└── __tests__/              # Vitest: stores e components
```

---

## 2. Nomenclatura

| Elemento | Convenção | Exemplo |
|---|---|---|
| Classe Java | PascalCase | `ChamadoService` |
| Método e variável Java | camelCase | `alterarStatus`, `usuarioAtual` |
| Constante Java | UPPER_SNAKE_CASE | `HEADER_REQUEST_ID` |
| Tabela e coluna do banco | snake_case | `chamado`, `solicitante_id`, `sla_horas` |
| Valor de enum | UPPER_SNAKE_CASE | `EM_ATENDIMENTO`, `SOLICITANTE` |
| Rota REST | kebab-case, no plural | `/api/chamados`, `/api/auth/usuarios-disponiveis` |
| Componente Vue | PascalCase (arquivo e uso) | `ChamadoAcoes.vue` |
| Store Pinia | camelCase com sufixo `Store` | `useChamadosStore` |
| Arquivo de service JS | camelCase | `chamados.js` |

Nomes de tabela são **singular** (`chamado`, `usuario`, `categoria`, `comentario`);
rotas são **plural** (`/chamados`). Não misture.

---

## 3. Regras de código — backend

### 3.1 Camadas

Dependência **unidirecional**: `web -> service -> domain <- repository`.

| Camada | Pode | Não pode |
|---|---|---|
| `web` | Falar com `service`, montar DTO, definir status HTTP | Conter regra de negócio; acessar `repository` |
| `service` | Orquestrar, falar com `domain` e `repository` | Conhecer `HttpServletRequest`, `ResponseEntity` ou qualquer tipo de `web` |
| `domain` | Regras puras | Conhecer Spring, HTTP ou repositórios |
| `repository` | Query e persistência | Conter regra de negócio |

Uma classe de `domain` **nunca** importa `org.springframework.*` (a única exceção são as
anotações JPA/Jakarta nas entidades, que são metadados de mapeamento, não Spring).

### 3.2 DTOs

- **Nunca** exponha uma entidade JPA num controller, nem na entrada nem na saída.
- Um DTO de entrada por operação, em `web/dto/request/`, sufixo `Request`.
- Um DTO de saída por representação, em `web/dto/response/`, sufixo `Response`.
- DTOs são `record` Java. São imutáveis e não têm lógica.
- Listagens usam um DTO "resumo" (`ChamadoResumoResponse`), o detalhe usa o completo
  (`ChamadoResponse`) — a lista não carrega comentários.
- O mapeamento é **manual**, em classes de `service/mapper/`. Sem MapStruct (ADR-006).

### 3.3 Validação

- Toda entrada valida com Bean Validation **no DTO**: `@NotBlank`, `@NotNull`, `@Size`,
  `@Email`, `@Positive`.
- O controller marca o parâmetro com `@Valid`.
- Validação de **formato** fica no DTO; validação de **regra** (categoria inativa, nome
  duplicado) fica no service e lança `RegraNegocioException`.

### 3.4 Tratamento de erro

- Erro é **exceção de domínio**, capturada por um único `@RestControllerAdvice`
  (`GlobalExceptionHandler`). Sem `try/catch` para controle de fluxo no service.
- O corpo da resposta é sempre `ErroResponse`, no formato da seção 8 de
  `business-rules.md`. Nenhum stack trace chega ao cliente.
- Mapa fixo: validação -> 400, `AcessoNegadoException` -> 403,
  `RecursoNaoEncontradoException` -> 404, `TransicaoInvalidaException` -> 422,
  `RegraNegocioException` -> 422, resto -> 500.
- 401 é escrito pelo `UsuarioAtualFilter`, que roda antes do `DispatcherServlet` e por
  isso não passa pelo advice.

### 3.5 Lombok

Permitido: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`,
`@RequiredArgsConstructor`, `@Slf4j`.

**Proibido `@Data` e `@EqualsAndHashCode` em entidade JPA** — geram `equals`/`hashCode`
sobre associações LAZY e causam inicialização acidental. Entidade define `equals`/
`hashCode` sobre o `id`, ou não define.

Injeção de dependência é por **construtor** (`@RequiredArgsConstructor` + campos
`private final`). Nunca `@Autowired` em campo.

---

## 4. Regras de código — frontend

- **Pages** montam tela e disparam ações de store. **Não** chamam Axios.
- **Stores** guardam estado e chamam services. É onde fica "recarregar a lista após ação".
- **Services** (`src/services/*.js`) são as únicas funções que montam URL de endpoint.
- **`src/services/api.js`** é o **único** arquivo que conhece o `baseURL` do backend, o
  header `X-User-Id` e o `X-Request-Id`. Nenhuma URL de backend hardcoded em qualquer
  outro arquivo.
- Um store Pinia por domínio: `auth`, `chamados`, `catalogo`. Stores usam a Setup API
  (`defineStore('nome', () => { ... })`).
- Componentes usam `<script setup>`. `props` declaradas com tipo e `required`.
- O router guard é UX: redireciona para `/login` sem sessão e para `/chamados` quando o
  perfil não está em `meta.perfis`. **A autorização de verdade é do backend.**
- Erro de API vira `q-notify` com a mensagem retornada pelo backend **e** o
  `X-Request-Id`, para correlacionar com o log.

---

## 5. Testabilidade

### 5.1 Decisões de design que são obrigatórias

| Regra | Por quê |
|---|---|
| Regras de domínio (`TransicaoStatus`, `SlaCalculator`, `PermissaoService`) são **classes puras**: sem `@Autowired` de repositório, sem `HttpServletRequest` | Testam-se com `new` + JUnit, em milissegundos, sem subir o Spring |
| **`java.time.Clock` injetado** em vez de `Instant.now()` | Teste de SLA e de `resolvidoEm` usa `Clock.fixed(...)`; nada de `Thread.sleep` |
| **`UsuarioAtual` chega por parâmetro** no service — nunca via `ThreadLocal`, `SecurityContextHolder` ou qualquer contexto estático | O teste monta o usuário na mão |
| Repositórios acessados **só via interface** Spring Data | Service testa com Mockito; a query real testa com `@DataJpaTest` |
| `ChamadoSpecification` é classe separada | O teste de visibilidade roda contra H2 de verdade |

Nenhuma classe de `domain` ou `service` pode chamar `Instant.now()`, `LocalDateTime.now()`
ou `new Date()`. Sempre `Instant.now(clock)`.

### 5.2 Convenções de teste

- **O nome do teste descreve a regra, não o método:**
  `deveRejeitarTransicaoDeAbertoParaResolvido` — nunca `testAlterarStatus2`.
- Padrão **given / when / then** nos testes de service, com os três blocos separados por
  linha em branco e comentados quando ajudar.
- Fixtures vêm de **builders** em `src/test/java/br/com/helpdesk/support/`
  (`ChamadoBuilder.aberto().comSolicitante(...).build()`).
- **Nunca reaproveite o `data.sql`** num teste automatizado. O seed é fixture de
  demonstração; teste que depende dele quebra quando a demo muda.
- Teste de integração usa o perfil `test`, H2 limpo, e `@Transactional` para rollback.
- Asserções com **AssertJ** (`assertThat(...)`), não `assertEquals`.
- `mvn test` e `npm run test` são pré-requisito de todo commit de etapa.

---

## 6. Observabilidade

- **Todo log via SLF4J** (`@Slf4j`). `System.out.println` é proibido em qualquer código
  de produção.
- Toda **transição de status** e toda **ação administrativa** (CRUD de usuário e de
  categoria) geram **uma linha `INFO` estruturada** com os campos:
  `requestId`, `usuarioId`, `perfil`, `acao`, `chamadoId`, `statusDe`, `statusPara`.
  `requestId` e `usuarioId` vêm do MDC (ficam no pattern do Logback, não na mensagem).
- **4xx** são logados em `WARN`, **sem stack trace**. **5xx** em `ERROR`, **com** stack
  trace. Esse é o único lugar que loga exceção.
- **Nenhum dado pessoal** em nível `INFO` ou acima além do **id** do usuário — nunca
  nome, e-mail, título ou descrição de chamado.
- `DEBUG` de SQL só no perfil `dev`.
- **Métricas só através de `MetricasNegocio`.** Nenhum service injeta `MeterRegistry`
  diretamente.

---

## 7. Commits

Formato: `tipo: descrição` — descrição em português, imperativo, minúscula, sem ponto final.

| Tipo | Quando |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `chore` | Build, dependências, configuração |
| `docs` | Documentação |
| `test` | Testes |

Exemplos: `feat: implementa maquina de estados do chamado`,
`test: cobre transicoes invalidas`, `docs: adiciona roteiro de demonstracao`.

Um commit por etapa do prompt de implementação.

---

## 8. Idioma

| O quê | Idioma |
|---|---|
| Nomes de classe, método, variável, tabela, coluna, rota | **Português** quando são termos do domínio (`Chamado`, `solicitanteId`, `/chamados`), que é o vocabulário do negócio |
| Palavras técnicas de estrutura | **Inglês** (`Service`, `Repository`, `Controller`, `Request`, `Response`, `Builder`, `findById`) |
| Textos de UI | **Português do Brasil** |
| Mensagens de erro para o usuário | **Português do Brasil** |
| Comentários e documentação | **Português do Brasil** |
| Mensagens de log | **Português do Brasil**, sem acento em chave estruturada |

O domínio é falado em português pelo negócio; traduzir `Chamado` para `Ticket` só criaria
um dicionário a mais para manter. O andaime técnico permanece em inglês porque é o
vocabulário do framework.
