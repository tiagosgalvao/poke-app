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
- [ ] **1.9 redis cache**: `@Cacheable` on `PokeApiClient`, typed JSON serializers, TTL, species trimming and `LoggingCacheErrorHandler`, with a test showing the second call hits the cache

## Phase 2: Local data (US03, US04, CRUD)

- [ ] **2.1 local schema migration** (all Phase 2 code lives in `com.poke.pokedex`): Flyway `V1__schema.sql` (users, local_pokemon, abilities, tags)
- [ ] **2.2 local pokemon domain**: `LocalPokemon` with proprietary fields and invariants, with tests
- [ ] **2.3 local pokemon service**: the `LocalPokemonRepository` domain interface and `PokedexService` (sync, import, get, list, update, patch, delete), with tests
- [ ] **2.4 jpa repository**: JPA entities, a Spring Data repository and the `LocalPokemonRepository` implementation in `pokedex.repository`/`pokedex.entity`, with `@DataJpaTest` (Testcontainers)
- [ ] **2.5 sync from pokeapi**: bounded batch, idempotent refresh that keeps proprietary fields, and a summary response
- [ ] **2.6 local pokemon endpoints** (request DTOs use Bean Validation; reassess [D20](DECISIONS.md#d20-validation-domain-guards-for-upstream-data-bean-validation-for-requests) here): `LocalPokemonController` (GET list/one, POST, PUT, PATCH, DELETE), with MVC tests for 400/404/409
- [ ] **2.7 optimistic locking**: a `version` check that surfaces as 409
- [ ] **2.8 local pokemon seed**: `V3__seed_local_pokemon.sql` with about 20 Pokemon, including proprietary data

## Phase 3: Authentication

- [ ] **3.1 auth domain and service** (all Phase 3 code lives in `com.poke.identity`): `User`, `UserRepository`, `PasswordHasher` and `TokenIssuer` domain interfaces, and `AuthService` (register, login), with tests
- [ ] **3.2 bcrypt and jwt support**: BCrypt hasher, HS256 token issuer and the JPA `users` repository
- [ ] **3.3 security config**: stateless; public GETs, protected mutations; ProblemDetail for 401/403
- [ ] **3.4 auth endpoints**: `AuthController`, with MVC tests (register 201/409/400, login 200/401)
- [ ] **3.5 demo users seed**: `V2__seed_users.sql` (`admin / Admin123!`, `ash / Pikachu123!`)
- [ ] **3.6 openapi bearer auth**: a bearer security scheme, so Swagger UI shows "Authorize"

## Phase 4: Backend hardening and packaging

- [ ] **4.1 end-to-end tests**: `@SpringBootTest` with Testcontainers (Postgres, Redis) and WireMock: register → login → sync → update → error paths
- [ ] **4.2 coverage review**: review the JaCoCo report and fill gaps in domain and services
- [ ] **4.3 api dockerfile**: multi-stage, non-root, layered jar, healthcheck
- [ ] **4.4 api in compose**: an `api` service that depends on healthy postgres and redis

## Phase 5: Frontend

- [ ] **5.1 web api client**: base fetch, ProblemDetail parsing, bearer header, 401 → logout
- [ ] **5.2 web auth**: Zustand store, login and register pages (RHF + zod), `<RequireAuth>`, header user menu
- [ ] **5.3 catalog page**: US01 responsive grid, pagination in the URL, skeletons, error state
- [ ] **5.4 detail page**: US02 artwork, stat bars, description, clickable evolution chain
- [ ] **5.5 my pokedex page**: US03/US04 list, sync dialog, edit form (proprietary fields, tags), delete with in-page confirm, inline 400/409 errors
- [ ] **5.6 web tests and console cleanup**: MSW-backed tests per feature; zero console warnings
- [ ] **5.7 web dockerfile and nginx**: SPA fallback and `/api` proxy; a `web` service in compose

## Phase 6: Delivery

- [ ] **6.1 final readme**: run instructions, screenshots, endpoint table, demo credentials
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
