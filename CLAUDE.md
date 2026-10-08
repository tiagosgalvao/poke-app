# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository.

## What this is

A monorepo for a Java technical exercise:

- A Spring Boot API that consumes PokeAPI and keeps a local Postgres replica with proprietary fields.
- A React SPA that consumes the API.

The design is in `docs/ARCHITECTURE.md` and the reasons behind it are in `docs/DECISIONS.md`. Add a new `D<n>` entry there whenever you make or change a technical decision. The work plan is in `docs/ROADMAP.md`. The upstream endpoints, field mapping and quirks are in `docs/POKEAPI.md`. Read them before making non-trivial changes.

## Layout

```
api/   Spring Boot 4.1 · Java 25 · Gradle Kotlin DSL   (package root: com.poke)
web/   React 19 · TypeScript · Vite 8 · Tailwind 4
docs/  ARCHITECTURE.md (design), DECISIONS.md (decision log), POKEAPI.md (upstream reference), CONVENTIONS.md (commit rules), ROADMAP.md (numbered tasks), GENAI.md (later)
docker-compose.yml   postgres:17 + redis:8 (api/web services added later)
```

## Commands

```bash
# infrastructure for local dev
docker compose up -d postgres redis

# api (run from api/)
./gradlew build              # compile + all tests + JaCoCo report (needs Docker for Testcontainers)
./gradlew test --tests '*ArchitectureTest'   # fast architecture check
./gradlew test --tests 'com.poke.application.*'   # a single package
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

- `domain` is pure Java. It has no Spring, JPA, Jackson or servlet imports, and it holds invariants and domain exceptions.
- `application` depends only on `domain`. It holds `port.in` (use-case interfaces), `port.out` (what the use cases need) and `service` (the implementations).
  - Services are **plain classes**, not annotated with `@Service`. They are wired as `@Bean`s in `config`.
- `adapter.in.web` holds controllers, request/response records, mappers and the `GlobalExceptionHandler`. It must not import `adapter.out`.
- `adapter.out.{pokeapi,persistence,security}` implement the `port.out` interfaces.
  - JPA `@Entity` classes live only in `adapter.out.persistence`, and are mapped to and from domain objects.
  - `@Cacheable` lives in the PokeAPI adapter.
- `config` handles wiring only. Nothing depends on it.
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
- **Validation:**
  - Bean Validation on request DTOs covers shape.
  - Domain constructors and methods cover business invariants.
- **Persistence:**
  - Flyway owns the schema, in `api/src/main/resources/db/migration/V{n}__desc.sql`. Never edit a migration that has been committed; add a new one.
  - `ddl-auto` stays `validate`.
- **Config:**
  - Configuration comes from env vars with defaults in `application.yml`, under the `poke.*` prefix.
  - Secrets come only from env. Never commit a real `.env`.
- **Web:**
  - Feature folders live under `src/features/{catalog,local,auth}`.
  - Server state goes through TanStack Query hooks. Don't keep API data in Zustand.
  - Auth state lives in the Zustand store.
  - Forms use react-hook-form + zod, with schemas that mirror the API rules.
  - Style with Tailwind utility classes, mobile-first.
  - The browser console must have no warnings: stable keys, labelled inputs, no act() warnings in tests.

## Workflow

- **TDD:** write or adjust the failing test first, then the code. Every new use case, adapter and controller gets tests:
  - unit tests for domain and application;
  - `@WebMvcTest` for controllers;
  - WireMock for PokeAPI;
  - `@DataJpaTest` + Testcontainers for persistence.
- Work one roadmap task at a time. Tasks are numbered `<phase>.<n>` in `docs/ROADMAP.md`.
- **Dependencies:**
  - Prefer versions that have been released for at least 2 weeks.
  - For npm, use `npm install --before=<date two weeks ago>`.
  - Gradle versions are pinned in `api/build.gradle.kts`.
- **Git:** do **not** commit or push unless explicitly asked. The user commits in parts. Staging is fine when asked.
- **Commits:** follow `docs/CONVENTIONS.md`. That means one commit per roadmap task, with the message `<task id> <short description>`, e.g. `0.1 monorepo layout`.
