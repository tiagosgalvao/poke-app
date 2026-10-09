# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository.

## What this is

A monorepo for a Java technical exercise:

- A Spring Boot API that consumes PokeAPI and keeps a local Postgres replica with proprietary fields.
- A React SPA that consumes the API.

The design is in `docs/ARCHITECTURE.md` and the reasons behind it are in `docs/DECISIONS.md`. Add a new `D<n>` entry there whenever you make or change a technical decision. The work plan is in `docs/ROADMAP.md`. The upstream endpoints, field mapping and quirks are in `docs/POKEAPI.md`. Read them before making non-trivial changes.

## Layout

```
api/   Spring Boot 4.1 · Java 25 · Gradle Kotlin DSL   (package root: com.poke, feature-first)
web/   React 19 · TypeScript · Vite 8 · Tailwind 4
docs/  ARCHITECTURE.md (design), DECISIONS.md (decision log), POKEAPI.md (upstream reference), CONVENTIONS.md (commits, code style, best practices), ROADMAP.md (numbered tasks), GENAI.md (GenAI write-up), E2E-TEST-PLAN.md (browser flows), screenshots/, demo/ (recordings)
docker-compose.yml   postgres:17 + redis:8 + api + web (nginx on :3000, proxies /api to the api service)
e2e/                 Playwright browser flows against the compose stack (see docs/E2E-TEST-PLAN.md)
.github/workflows/   CI: api.yml (gradle build + coverage gate), web.yml (lint, test, build), path-filtered
settings.gradle.kts  composite build including api/, so IDEs import Gradle from the repo root
```

## Commands

```bash
# whole stack in Docker (web http://localhost:3000, api http://localhost:8080)
docker compose up --build

# end-to-end check against the running stack (curl + jq)
scripts/smoke-test.sh

# browser flows with Playwright against the running stack (run from e2e/)
npm test                     # regression checks (project "checks"), headless
npm run demo                 # narrated demo chapters (project "demo"), recorded into docs/demo/*.mp4 (needs ffmpeg)

# infrastructure only, for local dev
docker compose up -d postgres redis

# api (run from api/)
./gradlew build              # compile + all tests + JaCoCo report (needs Docker for Testcontainers)
./gradlew test --tests '*ArchitectureTest'   # fast architecture check
./gradlew test --tests 'com.poke.catalog.*'   # a single feature
./gradlew bootRun            # http://localhost:8080, Swagger UI at /swagger-ui.html
# coverage report: api/build/reports/jacoco/test/html/index.html

# web (run from web/)
npm install
npm run dev                  # http://localhost:5173, proxies /api → :8080
npm test                     # vitest run
npm run lint                 # oxlint
npm run build                # tsc -b + vite build
```

## Backend architecture rules (enforced by `ArchitectureTest`)

Packages are **feature-first** (`com.poke.<feature>`), with the usual Spring layers inside each feature. See `docs/ARCHITECTURE.md` §4 and D4.

- **Features:** `catalog` (US01–US02, PokeAPI), `localpokemon` (US03–US04, Postgres), `identity` (users and auth), plus a `shared` kernel (`shared.exception`, `shared.pagination`, `shared.validation`, `shared.measure`, `shared.mapping`, `shared.config`).
- **`<feature>.domain`** is pure Java: no Spring, JPA, Jackson or servlet imports. It holds business types, invariants, domain exceptions, and the interfaces the feature needs from outside (e.g. `PokemonCatalog`, later `LocalPokemonRepository`).
- **`<feature>.service`** holds `@Service` classes (and `@Transactional` from Phase 2). They depend on `domain` only, never on `controller`, `client`, `repository` or `entity`.
- **`<feature>.client`** holds outbound HTTP (RestClient, DTOs, mapping, `@Cacheable`) and implements domain interfaces.
- **`<feature>.repository` / `<feature>.entity`** hold Spring Data JPA repositories and `@Entity` classes, mapped to and from domain objects, and implement domain interfaces.
- **`<feature>.security`** holds security infrastructure such as password hashing and token issuing (e.g. `identity.security`). It implements domain interfaces and follows the same rules as `client` and `repository`.
- **`<feature>.controller`** holds REST controllers and request/response records. Controllers call services only, with no logic and no `client`, `repository` or `entity` imports.
- **Mappings** that copy fields (domain → response, entity → domain) are MapStruct interfaces using `shared.mapping.MappingConfig`, in the `controller` or `entity` package (D21). Entity writes and `PokeApiMapper` stay hand-written.
- **Enums** go in an `enums` package inside the feature or layer that owns them (e.g. `catalog.client.enums`).
- **`shared`** depends on no feature. Features never form dependency cycles, and they talk to each other only through `service` classes.
- **Dependency inversion is for outbound I/O only.** No inbound use-case interfaces.
- If you need to break a rule, stop and discuss it. Don't weaken `ArchitectureTest`.

## Conventions

- **Java:**
  - Use records for DTOs and value objects; no Lombok.
  - Use constructor injection.
  - Use `final` fields and `Optional` only as a return type.
  - Indent with tabs, as in the generated sources (see `.editorconfig`).
- **API:**
  - Everything goes under `/api/v1`.
  - Collections return `PageResponse` (`content, page, size, totalElements, totalPages`), with `size` capped at 50.
  - Errors are RFC 9457 `ProblemDetail` with a `fieldErrors` extension for validation:
    - 400: bad input
    - 401: no or invalid token
    - 404: not found
    - 409: duplicate or stale `version`
    - 503: PokeAPI down
    - 500: generic, with no internals leaked
- **Validation (D20):**
  - Domain constructors guard invariants for data from any source, including upstream PokeAPI data. They use `shared.validation.Require` and value objects such as `PokemonKey`.
  - Request bodies use Bean Validation (`@Valid` + `@NotBlank`/`@Size`/`@Pattern`) at the controller. Violations return 400 with `fieldErrors`.
  - DTO limits reuse the domain constants (e.g. `ProprietaryData.MAX_TAGS`) so they never drift.
- **Persistence:**
  - Flyway owns the schema, in `api/src/main/resources/db/migration/V{n}__desc.sql`. Never edit a migration that has been committed; add a new one.
  - `ddl-auto` stays `validate`.
- **Config:**
  - Configuration comes from env vars with defaults in `application.yml`, under the `poke.*` prefix.
  - Secrets come only from env. Never commit a real `.env`.
- **Web:**
  - Feature folders live under `src/features/{catalog,local-pokemon,auth}`, mirroring the backend features.
  - Server state goes through TanStack Query hooks. Don't keep API data in Zustand.
  - Auth state lives in the Zustand store.
  - Forms use react-hook-form + zod, with schemas that mirror the API rules.
  - Style with Tailwind utility classes, mobile-first.
  - The browser console must have no warnings: stable keys, labelled inputs, no act() warnings in tests. The Vitest setup fails any test that logs a console error or warning.

## Workflow

- **TDD:** write or adjust the failing test first, then the code. Every new domain type, service, client/repository and controller gets tests:
  - unit tests for domain and services;
  - `@WebMvcTest` for controllers;
  - WireMock for PokeAPI;
  - `@DataJpaTest` + Testcontainers for persistence.
- Work one roadmap task at a time. Tasks are numbered `<phase>.<n>` in `docs/ROADMAP.md`.
- **Dependencies:**
  - Prefer versions that have been released for at least 2 weeks.
  - For npm, use `npm install --before=<date two weeks ago>`.
  - Gradle versions are pinned in `api/build.gradle.kts`.
- **Git:** do **not** commit or push unless explicitly asked. The user commits in parts. Staging is fine when asked.
- **Code style and best practices:** follow `docs/CONVENTIONS.md`. That means no redundant comments, no magic numbers or strings, static imports for constants and enums, enums in an `enums` package, and braces on every `if`.
- **Commits:** follow `docs/CONVENTIONS.md`. That means one commit per roadmap task, with the message `<task id> <short description>`, e.g. `0.1 monorepo layout`.
