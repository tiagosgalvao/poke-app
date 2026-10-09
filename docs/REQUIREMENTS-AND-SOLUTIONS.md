# Requirements and solutions

Every requirement of the exercise, how Poke App meets it, and where to check it: the code, the tests and the docs.

> **At a glance**
>
> | Area | Status |
> |---|---|
> | Functional requirements (PokeAPI integration, US01–US04) | ✅ 5 / 5 |
> | Technical requirements (mandatory, data, API, layers, testing) | ✅ 11 / 11 |
> | Frontend | ✅ 4 / 4 |
> | Submission and delivery | ✅ 5 / 5 |
> | Generative AI exercise | ✅ 4 / 4 |
> | Evaluation criteria | ✅ 6 / 6 |
>
> **Test suite**
> - **API:** 215 tests, at 99.4% line and 97.0% branch coverage. The build fails below 95% / 90%.
> - **Web:** 39 tests.
> - **Browser:** 11 Playwright checks against the running stack.
> - **CI:** three GitHub Actions workflows (`api`, `web`, `e2e`), all green on `main`.

Legend: ✅ done · ➕ beyond what was asked

---

## 1. Functional requirements

| # | Requirement | Solution | Evidence |
|---|---|---|---|
| F1 | **PokeAPI integration**: a Spring Boot REST API that talks to PokeAPI | ✅ **Fetching:** a `RestClient` with timeouts, behind a domain interface (`PokemonCatalog`).<br>**Speed:** virtual threads fetch the 41 upstream calls of a page concurrently.<br>**Errors:** a PokeAPI 404 becomes our 404, and a 5xx or timeout becomes our 503. | [`PokeApiClient`](../api/src/main/java/com/poke/catalog/client/PokeApiClient.java), [`PokeApiPokemonCatalog`](../api/src/main/java/com/poke/catalog/client/PokeApiPokemonCatalog.java), [POKEAPI.md](POKEAPI.md), [D10](DECISIONS.md#d10-restclient-for-pokeapi), [D19](DECISIONS.md#d19-virtual-threads-enabled-globally) |
| F2 | **US01: Pokemon enumeration.** Paginated results with sprite, category, mass and skills | ✅ `GET /api/v1/pokemon?page&size` returns `PageResponse` items with `spriteUrl`, `category` (the species genus), `weightKg` and `abilities` (with a hidden-ability flag). Page size is capped at 50. In the web app: a responsive card grid, with the page number in the URL. | [`PokemonController`](../api/src/main/java/com/poke/catalog/controller/PokemonController.java), [`CatalogPage`](../web/src/features/catalog/CatalogPage.tsx), [demo chapter 1](demo/01-catalog-and-details.mp4) |
| F2a | *Nice to have:* caching of service responses | ✅ Redis through Spring Cache, with a TTL from `CACHE_TTL`. If Redis goes down, the API falls back to PokeAPI. | [`PokeApiCacheConfig`](../api/src/main/java/com/poke/catalog/client/PokeApiCacheConfig.java), [D9](DECISIONS.md#d9-redis-via-spring-cache) |
| F3 | **US02: detailed view.** Image, core stats, narrative description, evolution lineage | ✅ `GET /api/v1/pokemon/{idOrName}` returns:<br>• the official artwork;<br>• the six base stats;<br>• the latest English flavor text;<br>• the whole evolution chain, flattened into stages with their triggers, so branching chains like Eevee's work.<br>The web app shows the stats as accessible progress bars and links each evolution to its page. | [`EvolutionChainFlattener`](../api/src/main/java/com/poke/catalog/client/EvolutionChainFlattener.java), [`PokemonDetailPage`](../web/src/features/catalog/PokemonDetailPage.tsx), [`EvolutionChain`](../web/src/features/catalog/EvolutionChain.tsx) |
| F4 | **US03: data synchronization.** Persist Pokemon to a local relational store that allows proprietary fields (localized names, geographical metadata, classification tags) | ✅ PostgreSQL with a Flyway schema:<br>• **Tables:** `local_pokemon`, plus ordered types and abilities and a tag set.<br>• **Proprietary fields:** `localizedName`, `region`, `habitat`, `tags` and `notes`.<br>• **Getting data in:** import one Pokemon (`POST /local-pokemon`), or sync a batch of up to 50 ids (`POST /local-pokemon/sync`).<br>• **Proprietary data is safe:** re-syncing refreshes the PokeAPI data but **never overwrites** proprietary data. | [`V1__schema.sql`](../api/src/main/resources/db/migration/V1__schema.sql), [`LocalPokemonService`](../api/src/main/java/com/poke/localpokemon/service/LocalPokemonService.java), [`LocalPokemon`](../api/src/main/java/com/poke/localpokemon/domain/LocalPokemon.java), [`SyncPanel`](../web/src/features/local-pokemon/SyncPanel.tsx) |
| F5 | **US04: local data modification.** Update any stored Pokemon. **404** for missing records, **400** for malformed payloads, and more defensive logic | ✅ `PUT` (replace) and `PATCH` (partial):<br>• **404** for an unknown id.<br>• **400** for malformed JSON, Bean Validation failures and bad parameters, with a `fieldErrors` list.<br>• ➕ **409** for a stale `version` (optimistic locking), so a concurrent edit is never silently lost.<br>• ➕ Domain guards: at most 10 normalized tags; text fields that are present can't be blank.<br>• ➕ Errors never leak internals. | [`LocalPokemonController`](../api/src/main/java/com/poke/localpokemon/controller/LocalPokemonController.java), [`LocalPokemonRequests`](../api/src/main/java/com/poke/localpokemon/controller/LocalPokemonRequests.java), [`ConcurrentUpdateTest`](../api/src/test/java/com/poke/localpokemon/repository/ConcurrentUpdateTest.java), [D8](DECISIONS.md#d8-optimistic-locking), [D20](DECISIONS.md#d20-validation-domain-guards-for-upstream-data-bean-validation-for-requests) |

## 2. Technical requirements

| # | Requirement | Solution | Evidence |
|---|---|---|---|
| T1 | Host the code in a **public Git repository** | ✅ Public on GitHub, with one commit per roadmap task. | [github.com/tiagosgalvao/poke-app](https://github.com/tiagosgalvao/poke-app), [ROADMAP.md](ROADMAP.md) |
| T2 | **Include tests** | ✅ Unit, slice (`@WebMvcTest`, `@DataJpaTest`), WireMock, Testcontainers (real Postgres and Redis), ArchUnit, an in-process end-to-end test, Vitest + MSW and Playwright. All of it runs in CI. | [§10 Testing strategy](ARCHITECTURE.md#10-testing-strategy), [E2E-TEST-PLAN.md](E2E-TEST-PLAN.md) |
| T3 | **Proper error handling** | ✅ RFC 9457 `ProblemDetail` for every error, from controllers and from security (401/403):<br>• **400** bad input, with `fieldErrors`;<br>• **401** no or invalid token;<br>• **404** not found;<br>• **409** duplicate or stale version;<br>• **503** PokeAPI down;<br>• **500** with a generic body. | [`GlobalExceptionHandler`](../api/src/main/java/com/poke/shared/exception/handler/GlobalExceptionHandler.java), [§6 API contract](ARCHITECTURE.md#6-api-contract), [D12](DECISIONS.md#d12-rfc-9457-problemdetail-errors) |
| T4 | *Nice to have:* a **caching layer for PokeAPI responses** | ✅ The same Redis cache as F2a. | [D9](DECISIONS.md#d9-redis-via-spring-cache) |
| T5 | A **front-end that consumes the API** | ✅ A React 19 + TypeScript SPA, served by nginx, which proxies `/api` to the API. | [web/README.md](../web/README.md), [`nginx.conf`](../web/nginx.conf) |
| T6 | **Database:** a primary entity and a secondary collection for user management. Records have a **unique primary key** and **at least two descriptive attributes** | ✅ **Primary entity:** `local_pokemon`, keyed by the PokeAPI national id (name, category, measures, types, abilities and proprietary fields).<br>**User collection:** `users`, with a UUID key, a unique username, a unique email, a BCrypt hash and a creation time. | [`V1__schema.sql`](../api/src/main/resources/db/migration/V1__schema.sql), [§5 Data model](ARCHITECTURE.md#5-data-model), [D7](DECISIONS.md#d7-pokeapi-id-as-the-local-primary-key) |
| T7 | **API:** full **CRUD** on the dataset, with standard HTTP verbs, required parameters and consistent return structures | ✅ **Create:** `POST /local-pokemon` (201 + body) and `POST /local-pokemon/sync`.<br>**Read:** `GET` list and one.<br>**Update:** `PUT` and `PATCH`.<br>**Delete:** `DELETE` (204).<br>Collections always return `PageResponse`, and errors always return `ProblemDetail`. Documented in OpenAPI / Swagger UI. | [README API table](../README.md#api), Swagger UI at `/swagger-ui.html` |
| T8 | An **auxiliary API** for registration, authentication and **protected vs public routes** | ✅ `POST /api/v1/auth/register` and `/login` (a JWT, HS256). Reads are public; every write needs a valid token. The token is checked in the security filter chain before any controller runs. | [`AuthController`](../api/src/main/java/com/poke/identity/controller/AuthController.java), [`SecurityConfig`](../api/src/main/java/com/poke/shared/config/SecurityConfig.java), [§8 Authentication](ARCHITECTURE.md#how-a-request-with-a-bearer-token-is-checked), [D11](DECISIONS.md#d11-minimal-jwt-auth) |
| T9 | A **data layer** for persistence, as the foundation for the controllers | ✅ A `repository` + `entity` package per feature: Spring Data JPA behind domain interfaces (`LocalPokemonRepository`, `UserRepository`), with MapStruct for entity → domain. | [`JpaLocalPokemonRepository`](../api/src/main/java/com/poke/localpokemon/repository/JpaLocalPokemonRepository.java), [`LocalPokemonEntity`](../api/src/main/java/com/poke/localpokemon/entity/LocalPokemonEntity.java), [D21](DECISIONS.md#d21-mapstruct-for-response-and-entity--domain-mappings) |
| T10 | A **business logic layer** for domain rules and validation, **independent of the API and data access** | ✅ `domain` is pure Java: no Spring, JPA or Jackson. `service` orchestrates the domain. **ArchUnit enforces the dependency rule**, so a violation fails the build. | [`ArchitectureTest`](../api/src/test/java/com/poke/ArchitectureTest.java), [§4 Backend architecture](ARCHITECTURE.md#4-backend-architecture-feature-first-clean-architecture), [D4](DECISIONS.md#d4-feature-first-packages-with-clean-layers-inside), [D5](DECISIONS.md#d5-services-as-service-beans-dependency-inversion-only-for-outbound-io) |
| T11 | **Thorough unit tests** for every core component | ✅ Every domain type, service, client, repository and controller has tests. A JaCoCo gate (95% line / 90% branch) runs in `./gradlew build` and in CI. | [`api.yml`](../.github/workflows/api.yml), [§10](ARCHITECTURE.md#10-testing-strategy) |

## 3. Frontend

| # | Requirement | Solution | Evidence |
|---|---|---|---|
| W1 | A **modern frontend framework** integrated with the backend | ✅ React 19, Vite, TypeScript, TanStack Query, React Router, Tailwind CSS. | [web/README.md](../web/README.md), [D14](DECISIONS.md#d14-vite--react--typescript) |
| W2 | **Responsive, user-centric design** | ✅ **Responsive:** a mobile-first grid.<br>**States:** skeleton loading, empty and error-with-retry on every query.<br>**Forms:** inline validation, an in-page delete confirm, and a "Reload latest" conflict flow.<br>**Accessibility:** labels and roles throughout. | [Screenshots](../README.md#screenshots), [demo videos](demo/) |
| W3 | **Standard CRUD** for the functional use cases | ✅ **Create:** sync by ids, or "Add to My Pokedex" from a detail page.<br>**Read:** catalog, detail and the local list.<br>**Update:** the edit form.<br>**Delete:** with a confirm step. | [`LocalPokemonPage`](../web/src/features/local-pokemon/LocalPokemonPage.tsx), [`EditLocalPokemonPage`](../web/src/features/local-pokemon/EditLocalPokemonPage.tsx), [demo chapter 3](demo/03-my-pokedex.mp4) |
| W4 | **Architectural integrity:** clean component organization and efficient state management | ✅ **Feature folders** mirror the backend.<br>**One owner per kind of state:** TanStack Query for server data, Zustand for the session, react-hook-form + zod for forms, and the URL for the page number. | [§9 Frontend](ARCHITECTURE.md#9-frontend-architecture), [Auth state with Zustand](ARCHITECTURE.md#auth-state-with-zustand), [D15](DECISIONS.md#d15-tanstack-query-for-server-state-zustand-for-auth) |

## 4. Submission and delivery

| # | Requirement | Solution | Evidence |
|---|---|---|---|
| S1 | A **comprehensive README**, with environment configuration and technical documentation | ✅ The README covers running it (Docker or local), configuration, credentials, the API, testing and the demo. Deeper docs: architecture, decisions, PokeAPI mapping, conventions, roadmap and the test plan. | [README.md](../README.md), [api/README.md](../api/README.md), [web/README.md](../web/README.md), [docs/](.) |
| S2 | **Seeded data or mock credentials** for the demo | ✅ **Users:** `ash / Pikachu123!` and `admin / Admin123!`.<br>**Pokemon:** 20 local Pokemon, some already with proprietary data. Flyway applies both on first start. | [`V2__seed_local_pokemon.sql`](../api/src/main/resources/db/migration/V2__seed_local_pokemon.sql), [`V3__seed_users.sql`](../api/src/main/resources/db/migration/V3__seed_users.sql) |
| S3 | **Pre-populated demonstration data** | ✅ Same as S2. The app is usable straight after `docker compose up`. | [README: Demo data](../README.md#demo-data-and-credentials) |
| S4 | A **Dockerfile** for containerized execution | ✅ Multi-stage `api/Dockerfile` (Temurin 25, non-root user, healthcheck) and `web/Dockerfile` (Node build + nginx). `docker-compose.yml` runs postgres, redis, api and web. The `e2e` CI job proves a clean `docker compose up --build`. | [`api/Dockerfile`](../api/Dockerfile), [`web/Dockerfile`](../web/Dockerfile), [`docker-compose.yml`](../docker-compose.yml), [`e2e.yml`](../.github/workflows/e2e.yml) |
| S5 | **Environment configuration** | ✅ Every setting comes from env vars with local defaults (`.env.example`). Secrets come only from the environment. | [`.env.example`](../.env.example), [README: Configuration](../README.md#configuration) |

## 5. Generative AI exercise

| # | Requirement | Solution | Evidence |
|---|---|---|---|
| G1 | The **prompt** you would use to generate a task-management REST API (CRUD; title, description, status, due_date; tasks belong to a user) | ✅ A structured prompt: pinned stack, domain rules, API contract, ownership, error format, tests first, and plan before code. | [GENAI.md: The prompt](GENAI.md#the-prompt) |
| G2 | The **output code**, or a representative sample | ✅ Migration, request and response records, service, controller and tests. | [GENAI.md: Representative output](GENAI.md#representative-output-after-review) |
| G3 | How you **validated** and **corrected** the AI's suggestions | ✅ Plan review, line-by-line reading, the build as referee, checking that tests can fail, and manual attempts to break it. Nine concrete corrections, from mass assignment and IDOR to timezone-safe dates. | [GENAI.md: How I validated](GENAI.md#how-i-validated-the-suggestions), [What I corrected](GENAI.md#what-i-corrected) |
| G4 | How you handled **edge cases, authentication and validation** | ✅ JWT on every route, ownership in the query (404, not 403), layered validation, an injected `Clock`, optimistic locking. | [GENAI.md: Edge cases](GENAI.md#edge-cases-authentication-and-validation) |
| ➕ | How GenAI was used to build *this* project | ✅ The guardrails, every decision I steered, the AI's mistakes and how they were caught, and how its autonomy is kept in check. | [GENAI.md Part 2](GENAI.md#part-2-how-genai-was-used-to-build-this-repository) |

## 6. Evaluation criteria

| # | Criterion | How the project answers it | Evidence |
|---|---|---|---|
| E1 | **Clean Architecture:** separation of concerns, independent components | Feature-first packages with clean layers inside. A framework-free domain, dependency inversion for outbound I/O, and no cycles between features, all enforced by ArchUnit. | [§4](ARCHITECTURE.md#4-backend-architecture-feature-first-clean-architecture), [`ArchitectureTest`](../api/src/test/java/com/poke/ArchitectureTest.java) |
| E2 | **Application testing:** sufficient coverage, TDD preferred | Test-first per roadmap task. 99.4% line / 97.0% branch coverage behind a 95/90 gate. Browser flows against the real stack. | [§10](ARCHITECTURE.md#10-testing-strategy), [CONVENTIONS.md](CONVENTIONS.md) |
| E3 | **Code quality:** organized, readable, best practices | Conventions written down and followed (records, constructor injection, no magic values, braces). Decisions logged with alternatives. MapStruct for mappings that only copy fields. | [CONVENTIONS.md](CONVENTIONS.md), [DECISIONS.md](DECISIONS.md) |
| E4 | **Functionality:** works as required, without bugs; *desired: no browser console warnings* | All flows verified in a real browser, in CI. **Both the unit tests and the Playwright checks fail on any console error or warning.** | [`web/src/test/setup.ts`](../web/src/test/setup.ts), [`e2e/tests/fixtures.ts`](../e2e/tests/fixtures.ts), [`e2e.yml`](../.github/workflows/e2e.yml) |
| E5 | **Presentation:** user stories, design choices, architecture, a live demo | A demo script, three narrated demo videos, and the architecture and decision docs to walk through. | [Demo script](ROADMAP.md#demo-script-for-the-presentation), [demo videos](demo/), [ARCHITECTURE.md](ARCHITECTURE.md) |
| E6 | **GenAI tools:** fluency, prompt engineering, critical thinking | The exercise write-up, plus a candid account of steering the AI on this project. | [GENAI.md](GENAI.md) |

---

## Verify it yourself in five minutes

```bash
git clone https://github.com/tiagosgalvao/poke-app && cd poke-app
docker compose up --build            # web: http://localhost:3000 · API: http://localhost:8080/swagger-ui.html
scripts/smoke-test.sh                # 24 API checks through the proxy (needs curl + jq)
cd e2e && npm ci && npx playwright install chromium && npm test   # 11 browser checks
```

Then log in as `ash / Pikachu123!` and follow the [demo script](ROADMAP.md#demo-script-for-the-presentation).
