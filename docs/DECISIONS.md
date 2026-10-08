# Decision log

Architecture Decision Records (ADR-style) for the Poke App. Each decision has a stable id (`D<n>`). Ids are never reused or renumbered: when a decision changes, mark it *Superseded* and add a new one. The design itself is described in [ARCHITECTURE.md](ARCHITECTURE.md).

## Index

| # | Decision | Area | Status |
|---|---|---|---|
| [D1](#d1-monorepo) | Monorepo (`api/` + `web/`) | Repo | Accepted |
| [D2](#d2-spring-boot-41--java-25) | Spring Boot 4.1 + Java 25 | Backend | Accepted |
| [D3](#d3-gradle-kotlin-dsl) | Gradle Kotlin DSL | Build | Accepted |
| [D4](#d4-hexagonal--clean-architecture-enforced-by-archunit) | Hexagonal / Clean Architecture, enforced by ArchUnit | Backend | Accepted |
| [D5](#d5-use-cases-as-plain-classes-wired-in-config) | Use cases as plain classes wired in `config` | Backend | Accepted |
| [D6](#d6-postgresql--flyway) | PostgreSQL + Flyway | Data | Accepted |
| [D7](#d7-pokeapi-id-as-the-local-primary-key) | PokeAPI id as the local primary key | Data | Accepted |
| [D8](#d8-optimistic-locking) | Optimistic locking (`version`) | Data | Accepted |
| [D9](#d9-redis-via-spring-cache) | Redis via Spring Cache | Caching | Accepted |
| [D10](#d10-restclient-for-pokeapi) | `RestClient` for PokeAPI | Integration | Accepted |
| [D11](#d11-minimal-jwt-auth) | Minimal JWT auth | Security | Accepted |
| [D12](#d12-rfc-9457-problemdetail-errors) | RFC 9457 ProblemDetail errors | API | Accepted |
| [D13](#d13-no-lombok-java-records) | No Lombok; Java records | Backend | Accepted |
| [D14](#d14-vite--react--typescript) | Vite + React + TypeScript (not Next.js) | Frontend | Accepted |
| [D15](#d15-tanstack-query-for-server-state-zustand-for-auth) | TanStack Query for server state, Zustand for auth | Frontend | Accepted |
| [D16](#d16-nginx-reverse-proxy-for-api) | nginx reverse proxy for `/api` | Runtime | Accepted |
| [D17](#d17-wiremock-for-pokeapi-in-tests) | WireMock for PokeAPI in tests | Testing | Accepted |
| [D18](#d18-dependencies-at-least-2-weeks-old) | Dependencies at least 2 weeks old | Supply chain | Accepted |
| [D19](#d19-virtual-threads-enabled-globally) | Virtual threads enabled globally | Backend / perf | Accepted |

---

### D1. Monorepo

- **Context:**
  - The spec asks for *a* public Git repository.
  - Reviewers should be able to clone it once and run everything.
- **Decision:** one repository, with `api/` (Spring Boot) and `web/` (React) side by side. A single root `docker-compose.yml` runs the whole stack.
- **Consequences:**
  - One `docker compose up --build`.
  - API and UI changes can land in the same commit.
  - One CI workflow (path-filtered jobs) and one README.
- **Alternatives considered:** two repositories (`poke-api`, `poke-web`). Rejected: the compose file would need a side-by-side clone, and contract changes would be split across repos.

### D2. Spring Boot 4.1 + Java 25

- **Context:** a modern Java/Spring stack was required. Java 25 is the current LTS, and Boot 4.1.1 is the stable default on start.spring.io.
- **Decision:** Spring Boot **4.1.1** on Java **25**.
- **Consequences:**
  - Records, pattern matching and sealed types are available.
  - Mature virtual threads are available (see D19).
  - Jakarta EE 11 baseline.
- **Alternatives considered:** Boot 4.0.x or 3.5.x on Java 21. Both work, but are older for no benefit.

### D3. Gradle Kotlin DSL

- **Context:** the user preferred Gradle, either Groovy or Kotlin DSL.
- **Decision:** Kotlin DSL (`build.gradle.kts`).
- **Consequences:**
  - Type-safe build scripts with IDE completion.
  - Versions pinned as `val`s at the top of the file.
- **Alternatives considered:** Groovy DSL, which is less type-safe, and Maven, which is more verbose.

### D4. Hexagonal / Clean Architecture, enforced by ArchUnit

- **Context:** the spec evaluates Clean Architecture and requires the business layer to be "independent from both the API and the data access components".
- **Decision:** the `domain` → `application` (ports + services) → `adapter` (in/out) → `config` layering.
- **Consequences:**
  - `ArchitectureTest` turns the dependency rule into failing tests:
    - no Spring, JPA or Jackson in `domain` or `application`;
    - `adapter.in` never imports `adapter.out`.
  - There's more mapping code (DTO ↔ domain ↔ entity). That's the price of independence.
- **Alternatives considered:** classic layered packages (`controller` / `service` / `repository`). Simpler, but entities leak everywhere and the rule isn't enforceable.

### D5. Use cases as plain classes wired in `config`

- **Context:** `@Service` on a use case makes `application` depend on Spring.
- **Decision:** services are plain Java classes, instantiated as `@Bean`s in `config`.
- **Consequences:**
  - Unit tests need no Spring context and stay fast.
  - Wiring is explicit, so it's visible in one place.
- **Alternatives considered:** component scanning with `@Service`. Less code, but it breaks D4.

### D6. PostgreSQL + Flyway

- **Context:**
  - US03 asks for a "local relational store".
  - The spec requires a primary entity plus a users collection, each with a PK and at least two descriptive attributes.
- **Decision:**
  - PostgreSQL 17 is the **source of truth**.
  - Flyway owns the schema and seed data.
  - Hibernate only validates (`ddl-auto: validate`).
- **Consequences:**
  - Reviewable, versioned SQL.
  - Schema drift fails at startup.
  - Tests use real Postgres through Testcontainers.
- **Alternatives considered:** H2 (behaves differently from production) and `ddl-auto: update` (not reviewable or repeatable).

### D7. PokeAPI id as the local primary key

- **Context:** the local store is a replica of upstream Pokemon, enriched with our own fields.
- **Decision:** `local_pokemon.id` = the PokeAPI national id.
- **Consequences:**
  - Re-syncing is idempotent.
  - Duplicate imports become **409**.
  - No id-mapping table.
- **Alternatives considered:** a surrogate UUID plus an `external_id` column. More flexible, but unnecessary for a replica.

### D8. Optimistic locking

- **Context:** US04 asks for "further defensive logic" on updates.
- **Decision:** a `version` column (`@Version`). Clients send it on `PUT`/`PATCH`, and a stale version returns **409**.
- **Consequences:**
  - Concurrent edits can't silently overwrite each other.
  - The client must round-trip `version`.
- **Alternatives considered:** last-write-wins (data loss) and pessimistic locks (overkill for a web API).

### D9. Redis via Spring Cache

- **Context:**
  - Caching PokeAPI responses is a nice-to-have.
  - PokeAPI asks consumers to cache.
  - The data is effectively static (upstream `max-age=86400`).
- **Decision:**
  - `@Cacheable` in the PokeAPI adapter, backed by Redis 8 (JSON values, 24 h TTL).
  - Cache per upstream resource (`pokemon`, `species`, `evolution-chain`, `pokemon-page`).
  - A `CacheErrorHandler` falls back to PokeAPI when Redis is down.
- **Consequences:**
  - Shared across instances.
  - Visible in the demo (`redis-cli KEYS '*'`).
  - Redis is never required for correctness.
- **Alternatives considered:** Caffeine, which is in-process and simpler but per-instance and invisible.

### D10. `RestClient` for PokeAPI

- **Context:** the API calls PokeAPI on every cold cache miss, and on US01 that's 1 + 2 × page size calls (see [POKEAPI.md](POKEAPI.md#3-get-pokemon-list)).
- **Decision:** Spring's synchronous `RestClient` with explicit connect and read timeouts (2 s / 5 s). Concurrency comes from virtual threads (D19), not from a reactive client.
- **Consequences:**
  - Straightforward, debuggable code.
  - Easy to test with WireMock.
  - Upstream errors map to domain exceptions (404 → not found; timeout or 5xx → 503).
- **Alternatives considered:**
  - WebFlux `WebClient`: reactive types spread through the code.
  - OpenFeign: an extra dependency for a handful of endpoints.

### D11. Minimal JWT auth

- **Context:** the spec requires user registration, authentication and public vs protected routes, and nothing more.
- **Decision:**
  - `register` / `login` endpoints with BCrypt hashes.
  - HS256 JWTs validated by Spring's built-in `oauth2-resource-server`.
  - A single role: reads are public; sync and writes need a token.
- **Consequences:**
  - The requirement is met with very little security code.
  - No refresh tokens, revocation or roles. Documented as out of scope.
- **Alternatives considered:**
  - HTTP Basic: the SPA would have to store credentials.
  - Sessions + CSRF: stateful.
  - A full OAuth provider: overkill.

### D12. RFC 9457 ProblemDetail errors

- **Context:** the spec asks for proper error handling and consistent return structures.
- **Decision:**
  - All errors are `application/problem+json` (Spring `ProblemDetail`), with a `fieldErrors` extension for validation.
  - 401/403 use the same shape, through custom security handlers.
- **Consequences:** one error shape for the frontend to parse; a standard that reviewers recognize.
- **Alternatives considered:** a custom error envelope, which would be non-standard.

### D13. No Lombok; Java records

- **Context:** modern Java removes most of the boilerplate Lombok was used for.
- **Decision:** records for DTOs and value objects; explicit code elsewhere.
- **Consequences:**
  - No annotation-processor magic to explain in the code review.
  - JPA entities are slightly more verbose.
- **Alternatives considered:** Lombok, which is widely used but hides generated code.

### D14. Vite + React + TypeScript

- **Context:** the spec suggests React or Vue, and the app is a client of an existing API.
- **Decision:** a Vite 8 SPA with React 19 and TypeScript.
- **Consequences:**
  - Fast dev loop.
  - Static build served by nginx.
  - No SSR.
- **Alternatives considered:** Next.js. Its SSR and routing conventions add nothing for an authenticated SPA.

### D15. TanStack Query for server state, Zustand for auth

- **Context:** the spec evaluates "efficient state management".
- **Decision:** each kind of state has one owner:
  - server data: TanStack Query;
  - auth session: Zustand (persisted);
  - form input: react-hook-form + zod;
  - page and filters: the URL.
- **Consequences:**
  - No hand-written caching or loading flags.
  - No duplicated state.
- **Alternatives considered:**
  - Redux Toolkit / RTK Query: heavier.
  - Context only: re-render issues and manual caching.

### D16. nginx reverse proxy for `/api`

- **Context:** the browser should not need cross-origin calls.
- **Decision:** the web image's nginx serves the SPA and proxies `/api` to `api:8080`. In dev, the Vite proxy does the same.
- **Consequences:**
  - Same origin in every mode, so there's no CORS config to maintain.
  - A CORS allowlist is only needed if the API is called from another origin.
- **Alternatives considered:** CORS on the API, which is more configuration and more failure modes.

### D17. WireMock for PokeAPI in tests

- **Context:**
  - Adapter tests must cover mapping, 404, 5xx and timeouts.
  - CI must not depend on the live PokeAPI.
- **Decision:** WireMock serving trimmed real responses (fixtures in `src/test/resources/pokeapi/`).
- **Consequences:**
  - Real HTTP round-trips, deterministic results, no network.
  - Fixtures must be refreshed if upstream changes shape.
- **Alternatives considered:** `MockRestServiceServer`, which has no real socket and can't simulate timeouts realistically.

### D18. Dependencies at least 2 weeks old

- **Context:** supply-chain hygiene. Freshly published packages are the riskiest.
- **Decision:** prefer versions released at least 2 weeks ago.
  - npm installs use `--before=<date>`.
  - Gradle versions are pinned explicitly (e.g. ArchUnit 1.5.0 instead of 1.5.1).
- **Consequences:** slightly behind the latest releases, and every upgrade is deliberate.
- **Alternatives considered:** always-latest. Faster, but riskier.

### D19. Virtual threads enabled globally

- **Status:** Accepted.
- **Context:**
  - US01 is I/O-bound by nature. PokeAPI's list endpoint returns only names and URLs, so one page of *n* Pokemon needs **1 + 2n** upstream calls: `/pokemon/{id}` and `/pokemon-species/{id}` per entry. That's **41 calls** for a 20-item page.
  - Made sequentially at around 100–200 ms each, a cold page would take several seconds.
  - Every request also blocks on Postgres and Redis.
- **Decision:**
  - Set `spring.threads.virtual.enabled: true` in `application.yml`. Tomcat request handling and Spring's task executors (`@Async`, scheduling) then run on **virtual threads** (JDK 21+ Project Loom).
  - The US01 fan-out (roadmap task 1.6) uses `Executors.newVirtualThreadPerTaskExecutor()` to issue the per-item calls **concurrently** with plain blocking code. The page then takes about as long as its slowest call, not the sum of all calls.
- **Why it's safe here:**
  - **Java 25:** the old "pinning" problem (a virtual thread blocked inside `synchronized` holding its carrier OS thread) was fixed in JDK 24 ([JEP 491](https://openjdk.org/jeps/491)).
  - **Bounded downstreams:** HikariCP still caps real database connections, so cheap threads can't flood Postgres.
  - **Bounded fan-out:** page `size` is capped at 50 and sync batches at 50 ids. Combined with the Redis cache (D9), that limits the load we put on PokeAPI.
  - **Turning it off is a one-line change:** removing the property falls back to Tomcat's platform-thread pool with no code changes.
- **Consequences:**
  - High concurrency for blocking I/O without the reactive programming model.
  - Code stays synchronous, readable and testable (D10).
  - Stack traces and debugging stay normal.
  - `ThreadLocal`-heavy libraries create one copy per virtual thread. That's fine for our stack, but worth knowing.
- **Alternatives considered:**
  - **WebFlux / `WebClient` (reactive):** non-blocking, but `Mono`/`Flux` spreads through adapters and tests and is harder to read and explain.
  - **A tuned platform thread pool (`ThreadPoolTaskExecutor`):** works, but it needs pool sizing and invites thread starvation under load.
  - **Sequential calls with caching only:** simplest, but the first load of every page would be slow.
