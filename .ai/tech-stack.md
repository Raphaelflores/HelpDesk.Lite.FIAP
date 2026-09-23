# Tech Stack — HelpDesk Lite

Versões permitidas e comandos oficiais do projeto. Este arquivo é normativo: **nada fora
desta lista entra no projeto sem aprovação explícita**.

---

## 1. Backend

| Item | Versão fixada | Papel |
|---|---|---|
| Java (JDK) | **21** (LTS) | Linguagem e runtime. `maven.compiler.release = 21` |
| Spring Boot | **3.3.5** | Parent POM e gestão de dependências |
| Spring Web (`spring-boot-starter-web`) | herdada do parent | Controllers REST, Tomcat embutido |
| Spring Data JPA (`spring-boot-starter-data-jpa`) | herdada do parent | Repositórios, `Specification`, `Pageable` |
| Bean Validation (`spring-boot-starter-validation`) | herdada do parent | `@NotBlank`, `@NotNull`, `@Size` nos DTOs de entrada |
| H2 Database | herdada do parent (`runtime`) | Banco em memória do MVP + console web |
| springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | **2.6.0** | OpenAPI 3 + Swagger UI |
| Lombok | **1.18.34** | `@Getter`, `@Setter`, `@Builder`, `@Slf4j` (escopo `provided`) |
| Spring Boot Actuator | herdada do parent | `health`, `metrics`, `prometheus` |
| Micrometer — registry Prometheus (`micrometer-registry-prometheus`) | herdada do parent | Métricas técnicas e de negócio |
| Maven | **3.9.x** (wrapper `mvnw` incluso) | Build |

> **Nota de ambiente.** O projeto compila com `release 21`. Rodar com um JDK mais novo
> (24, por exemplo) funciona, mas o JDK homologado é o 21 — é o que o `mvnw` espera
> encontrar em `JAVA_HOME`.

## 2. Testes do backend

| Item | Versão | Papel |
|---|---|---|
| JUnit 5 (Jupiter) | via `spring-boot-starter-test` | Runner |
| Mockito | via `spring-boot-starter-test` | Mock de repositórios nos testes de service |
| AssertJ | via `spring-boot-starter-test` | Asserções fluentes |
| Spring Boot Test | via `spring-boot-starter-test` | `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` + `MockMvc` |
| JaCoCo Maven Plugin | **0.8.12** | Cobertura, com `check` amarrado à fase `verify` |

**Meta de cobertura obrigatória (falha o build):** 80 % de linhas em
`br.com.helpdesk.domain.*` e `br.com.helpdesk.service.*`.
Controllers, configuração e frontend **não** têm meta numérica no MVP.

## 3. Frontend

| Item | Versão fixada | Papel |
|---|---|---|
| Node.js | **20 LTS ou superior** (validado em 24 LTS) | Runtime de build |
| Vue | **^3.5.12** (satisfaz o mínimo 3.4) | Framework SPA |
| Quasar | **^2.17.0** | Biblioteca de componentes e CLI |
| `@quasar/app-vite` | **^2.0.0** | Build/dev server (Vite) do Quasar CLI |
| `@quasar/extras` | **^1.16.12** | Ícones Material e fonte Roboto |
| Pinia | **^2.2.4** | Estado da aplicação |
| Vue Router | **^4.4.5** | Rotas + guard de perfil |
| Axios | **^1.7.7** | Cliente HTTP |

## 4. Testes do frontend

| Item | Versão | Papel |
|---|---|---|
| Vitest | **^2.1.3** | Runner |
| `@vue/test-utils` | **^2.4.6** | Montagem de componentes |
| `@vitejs/plugin-vue` | **^5.1.4** | Compila SFCs dentro do Vitest |
| `@quasar/vite-plugin` | **^1.8.0** | Auto-import de componentes Quasar dentro do Vitest |
| `jsdom` | **^25.0.1** | Ambiente DOM |

> `@vitejs/plugin-vue`, `@quasar/vite-plugin` e `jsdom` são **ferramental de teste**, não
> dependências de aplicação: existem só para o `vitest.config.js` conseguir montar um SFC
> Quasar fora do `quasar dev`. Não use nenhum deles em código de `src/`.

---

## 5. Proibido sem aprovação explícita

Adicionar qualquer item abaixo exige parar, reportar e aguardar decisão humana:

| Proibido | Motivo |
|---|---|
| **Spring Security** ou qualquer lib de segurança/JWT | ADR-003: a autenticação do MVP é simulada por `X-User-Id` num filter. Segurança real é evolução |
| **Qualquer banco além do H2** (PostgreSQL, MySQL, SQL Server, Mongo…) | ADR-008: o MVP roda sem instalar nada |
| **Qualquer lib de UI além do Quasar** (Vuetify, Element Plus, Tailwind, Bootstrap…) | Uma biblioteca de componentes só |
| **Qualquer state management além do Pinia** (Vuex, Zustand…) | Um padrão de estado só |
| **MapStruct** ou qualquer gerador de mapeamento | ADR-006: mapeamento manual, poucas classes |
| **Spring StateMachine** | ADR-005: a máquina de estados é uma tabela em `TransicaoStatus` |
| **Flyway / Liquibase** | O seed do MVP é o `data.sql` |
| **Lombok `@Data` em entidade JPA** | Gera `equals`/`hashCode` sobre associações LAZY |
| **Cypress / Playwright** | E2E está fora do MVP (seção 4.12 do documento) |
| **Docker / docker-compose** | Execução 100 % local, dois terminais |

---

## 6. Comandos oficiais

### Backend (executar em `backend/`)

| Comando | O que faz |
|---|---|
| `mvn spring-boot:run` | Sobe a API em **http://localhost:8080** (perfil `dev`) |
| `mvn test` | Só os testes |
| `mvn verify` | Testes + relatório JaCoCo + **verificação dos 80 %** |
| `mvn clean package` | Gera o `.jar` em `target/` |

> Há um Maven Wrapper no repositório: `./mvnw` (Linux/macOS) e `mvnw.cmd` (Windows)
> funcionam sem Maven instalado. Onde este arquivo escreve `mvn`, leia "`mvn` ou `./mvnw`".

### Frontend (executar em `frontend/`)

| Comando | O que faz |
|---|---|
| `npm install` | Instala dependências |
| `npx quasar dev` | Sobe a SPA em **http://localhost:9000** com hot reload |
| `npm run test` | Vitest (modo run, sem watch) |
| `npx quasar build` | Build de produção em `frontend/dist/spa` |

---

## 7. URLs de operação (perfil `dev`)

| URL | O que é |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI — documentação viva e execução manual dos endpoints |
| `http://localhost:8080/v3/api-docs` | Especificação OpenAPI em JSON |
| `http://localhost:8080/h2` | Console do H2 (JDBC URL `jdbc:h2:mem:helpdesk`, usuário `sa`, senha vazia) |
| `http://localhost:8080/actuator/health` | Health check (inclui o indicador do banco) |
| `http://localhost:8080/actuator/metrics` | Lista de métricas disponíveis |
| `http://localhost:8080/actuator/prometheus` | Métricas em formato Prometheus, incluindo as `helpdesk_*` |
| `http://localhost:9000` | SPA |

Em `prod` só `health` e `prometheus` ficam expostos; console H2 e Swagger são desligados.
