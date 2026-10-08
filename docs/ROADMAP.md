# Roadmap / Next steps

The work is split into phases, and each phase into numbered tasks (`<phase>.<n>`). See [ARCHITECTURE.md](ARCHITECTURE.md) for the design. Each task is one commit, and its **bold** text is the commit message (see [CONVENTIONS.md](CONVENTIONS.md#commit-convention)).

## Phase 0: Foundation

- [x] **0.1 monorepo layout**: root `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`
- [x] **0.2 api bootstrap**: start.spring.io scaffold (Boot 4.1.1, Java 25, Gradle Kotlin DSL). It adds springdoc, ArchUnit, WireMock and JaCoCo, uses an env-driven `application.yml`, pins Testcontainers images, and includes `api/README.md` (setup, configuration, dependencies explained).
- [x] **0.3 architecture rules**: `ArchitectureTest` (ArchUnit) enforces the dependency rule.
- [x] **0.4 web bootstrap**: Vite 8 + React 19 + TS. Includes Tailwind 4, React Router, TanStack Query, RHF + zod, Zustand, Vitest + Testing Library + MSW, an app shell and a smoke test, and `web/README.md` (scripts, structure, libraries explained).
- [x] **0.5 docker compose**: Postgres 17 and Redis 8, both with healthchecks.
- [x] **0.6 project docs**: root `README.md`, `CLAUDE.md` and `docs/` (ARCHITECTURE, DECISIONS, POKEAPI, CONVENTIONS, ROADMAP).

## Phase 1: Catalog (US01, US02)

All Phase 1 code lives in `com.poke.catalog` and `com.poke.shared` ([D4](DECISIONS.md#d4-feature-first-packages-with-clean-layers-inside)).

- [x] **1.1 feature-first layout**: `ArchitectureTest` rules for feature-first packages (`domain`, `service`, `client`, `repository`, `entity`, `controller`), design docs, and a root `settings.gradle.kts` so IDEs import Gradle from the repo root
- [x] **1.2 shared kernel**: `shared.exception` (DomainException hierarchy), `shared.pagination` (`Page`, `PageRequest`), `shared.validation` (`Require`), with tests
- [x] **1.3 catalog domain model**: `PokemonSummary`, `PokemonDetail`, `Ability`, `Stat`, `EvolutionStage`, `PokemonKey`, `PokemonNotFoundException` and the `PokemonCatalog` interface, with tests
- [x] **1.4 pokeapi client**: `PokeApiClient` (RestClient, timeouts, 404 → empty, failures → 503), DTOs, mapper and `PokeApiPokemonCatalog`, with WireMock tests and fixtures (see [POKEAPI.md](POKEAPI.md))
- [x] **1.5 catalog service**: `CatalogService` (browse, getDetail with key normalization and not-found), with unit tests mocking `PokemonCatalog`
- [x] **1.6 evolution chain and flavor text mapping**: flatten the chain while keeping branches; normalize the flavor text
- [x] **1.7 parallel page fetch**: fan out per-item calls for list pages on virtual threads ([D19](DECISIONS.md#d19-virtual-threads-enabled-globally))
- [x] **1.8 catalog endpoints**: `PokemonController`, `PokemonResponses`, `PageResponse`, `GlobalExceptionHandler` (ProblemDetail) and `SecurityConfig`, with `@WebMvcTest`
- [x] **1.9 redis cache**: `@Cacheable` on `PokeApiClient`, typed JSON serializers, TTL, species trimming and `LoggingCacheErrorHandler`, with a test showing the second call hits the cache

## Phase 2: Local data (US03, US04, CRUD)

- [x] **2.1 local schema migration** (all Phase 2 code lives in `com.poke.localpokemon`): Flyway `V1__schema.sql` (users, local_pokemon, types, abilities, tags), with a Testcontainers migration test
- [x] **2.2 local pokemon domain**: `LocalPokemon` aggregate (import, refresh that never touches proprietary data, version check), `UpstreamData`, `ProprietaryData` (at most 10 normalized tags), `ProprietaryPatch`, `LocalPokemonRepository` and the not-found/conflict exceptions, with tests
- [x] **2.3 jpa repository**: `LocalPokemonEntity` (`Persistable` for assigned ids, `@Version`, ordered type/ability collections, tag set), the Spring Data `LocalPokemonJpaRepository` and `JpaLocalPokemonRepository` implementing the domain interface, with `@DataJpaTest` (Testcontainers)
- [x] **2.4 local pokemon service**: `LocalPokemonService` (import via `CatalogService`, get, list, update, patch, delete; `@Transactional`), a `Clock` bean, and `spriteUrl` added to the catalog detail so imports copy it, with Mockito tests
- [x] **2.5 sync from pokeapi**: `SyncBatch` (1–50 distinct positive ids, list or range), `LocalPokemonService.sync` creating or refreshing each Pokemon without touching proprietary data, unknown ids reported as failed, all-or-nothing on a PokeAPI outage, and a `SyncSummary`
- [x] **2.6 local pokemon endpoints**: `LocalPokemonController` (GET list/one, POST import, PUT, PATCH, DELETE, POST sync), request DTOs with Bean Validation, ProblemDetail 409 for conflicts and 400 with `fieldErrors` ([D20](DECISIONS.md#d20-validation-domain-guards-for-upstream-data-bean-validation-for-requests) reassessed), and a shared `Measures` helper, with `@WebMvcTest`
- [x] **2.7 optimistic locking**: a stale `version` (service check) and a lost update between two concurrent writers (JPA `@Version`, translated by `JpaLocalPokemonRepository`) both surface as `StaleVersionException` → 409, with a two-transaction test and an MVC test
- [x] **2.8 local pokemon seed**: `V2__seed_local_pokemon.sql` with 20 Kanto Pokemon (real PokeAPI data), 8 of them with proprietary data (Japanese name, region, habitat, tags, notes), plus seed-safe migration and concurrency tests

## Phase 3: Authentication

All Phase 3 code lives in `com.poke.identity`.

- [x] **3.1 identity domain**: `User` (normalized username and email), `RawPassword` (8–72 characters, never printed), `AccessToken`, the `UserRepository`, `PasswordHasher` and `TokenIssuer` interfaces, and the conflict/unauthorized exceptions, with tests
- [x] **3.2 password, token and user storage**: BCrypt `PasswordHasher`, HS256 JWT `TokenIssuer` (Nimbus), and the JPA `users` repository, with tests
- [x] **3.3 auth service**: `AuthService` (register with duplicate checks, login with one generic error for unknown user or wrong password), with Mockito tests
- [x] **3.4 security config**: stateless resource server validating the HS256 JWT; public GETs and auth endpoints, protected mutations and sync; ProblemDetail 401/403
- [x] **3.5 auth endpoints**: `AuthController` (`POST /api/v1/auth/register` 201/400/409, `POST /api/v1/auth/login` 200/401), with `@WebMvcTest`
- [x] **3.6 demo users seed**: `V3__seed_users.sql` (`admin / Admin123!`, `ash / Pikachu123!`)
- [x] **3.7 openapi bearer auth**: a bearer security scheme, so Swagger UI shows "Authorize"

## Phase 4: Backend hardening and packaging

- [x] **4.1 end-to-end tests**: `PokeAppEndToEndTest`, a `@SpringBootTest` through the real security chain with Testcontainers (Postgres, Redis) and WireMock: register → login → 401 without a token → import → 409 duplicate → patch → 409 stale version → 400 `fieldErrors` → sync (refreshed and failed) → delete → 404
- [x] **4.2 coverage review**: filled the gaps the JaCoCo report showed (403 handler, incomplete sync request, unexpected fetch failure, `User` null inputs) and added a coverage gate to `check` (95% line, 90% branch; currently about 99% / 97%)
- [x] **4.3 api dockerfile**: multi-stage (JDK build with a Gradle cache mount, JRE runtime), extracted Spring Boot layers, non-root `poke` user, `/actuator/health` healthcheck, `.dockerignore`, and a fixed `poke-api.jar` name
- [x] **4.4 api in compose**: an `api` service built from `api/Dockerfile`, configured through the same env vars as `.env.example`, waiting for healthy postgres and redis

## Phase 5: Frontend

- [x] **5.1 web api client**: `apiRequest` (bearer header, JSON, 204, network errors), ProblemDetail → typed `ApiError` with `fieldErrors`, a 401 callback for logout, typed API models, and MSW test setup
- [x] **5.2 web auth**: persisted Zustand session (expiry-aware) wired into the API client (401 → logout), login and register pages (RHF + zod mirroring the API rules, server errors inline, register logs straight in), `<RequireAuth>` with redirect back, and a header with navigation and the user menu
- [x] **5.3 catalog page**: US01 responsive card grid (sprite, number, category, weight, ability chips with hidden ones marked), pagination in the URL (`?page=`, previous data kept while loading), skeleton cards, error state with retry
- [x] **5.4 detail page**: US02 artwork, types, measures, abilities, accessible stat bars, description, an evolution chain grouped by stage (branches side by side, current Pokemon highlighted, triggers shown), and a not-found page
- [x] **5.5 my pokedex page**: US03/US04 `/my-pokedex` list with proprietary data, a sync panel (ids, summary), an edit page (proprietary fields and tags, server 400 errors on their fields, 409 with "reload latest"), delete with an in-page confirm, and "Add to My Pokedex" on the detail page
- [x] **5.6 web tests and console cleanup**: tests now fail on any console error or warning (this caught and fixed `act()` warnings from session resets), plus tests for logout, expired sessions, pagination and formatting. 36 MSW-backed tests in total.
- [x] **5.7 web dockerfile and nginx**: SPA fallback and `/api` proxy; a `web` service in compose

## Phase 6: Delivery

- [x] **6.1 final readme**: run instructions, screenshots, endpoint table, demo credentials
- [ ] **6.2 genai write-up**: `docs/GENAI.md` covering the Task-management API prompt, a representative output, validation and corrections, and edge cases, auth and validation; plus how GenAI was used on this project
- [ ] **6.3 github actions ci**: an `api` job (Gradle build + tests + JaCoCo) and a `web` job (lint + test + build), filtered by path
- [ ] **6.4 fresh clone smoke test**: `docker compose up --build` from a clean clone, then the demo script below

## Demo script (for the presentation)

1. `docker compose up --build`, then open http://localhost:3000.
2. Browse pages 1 and 2. Reload page 1 and point out that it is now faster because it comes from Redis (`docker compose exec redis redis-cli KEYS '*'`).
3. Open Eevee to show the branching evolution chain, then Pikachu to show stats and description.
4. Try *My Pokedex* while logged out, which redirects to login. Log in as `ash / Pikachu123!`.
5. Sync ids 1–10 and show the created/refreshed summary.
6. Edit Bulbasaur: set the localized name, region and tags. Show the validation error for a blank name, then save.
7. Trigger a 409 by editing in two tabs. Delete a record.
8. In Swagger UI, show the 404/400/401 ProblemDetail bodies.
9. Walk through the code: `ArchitectureTest`, a service unit test, and the WireMock client test. Show the JaCoCo report.
