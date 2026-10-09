# Decision log

Architecture Decision Records (ADR-style) for the Poke App. Each decision has a stable id (`D<n>`). Ids are never reused or renumbered: when a decision changes, mark it *Superseded* and add a new one. The design itself is described in [ARCHITECTURE.md](ARCHITECTURE.md).

## Index

| # | Decision | Area | Status |
|---|---|---|---|
| [D1](#d1-monorepo) | Monorepo (`api/` + `web/`) | Repo | Accepted |
| [D2](#d2-spring-boot-41--java-25) | Spring Boot 4.1 + Java 25 | Backend | Accepted |
| [D3](#d3-gradle-kotlin-dsl) | Gradle Kotlin DSL | Build | Accepted |
| [D4](#d4-feature-first-packages-with-clean-layers-inside) | Feature-first packages with clean layers inside, enforced by ArchUnit | Backend | Accepted |
| [D5](#d5-services-as-service-beans-dependency-inversion-only-for-outbound-io) | Services as `@Service` beans; dependency inversion only for outbound I/O | Backend | Accepted |
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
| [D20](#d20-validation-domain-guards-for-upstream-data-bean-validation-for-requests) | Validation: domain guards for upstream data, Bean Validation for requests | Backend | Accepted (reassess in Phase 2) |
| [D21](#d21-mapstruct-for-response-and-entity-to-domain-mappings) | MapStruct for response and entity → domain mappings | Backend | Accepted |

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

### D4. Feature-first packages with clean layers inside

- **Context:**
  - The spec evaluates Clean Architecture and requires the business layer to be "independent from both the API and the data access components".
  - Clean Architecture only requires the **dependency rule**: business rules independent of frameworks, UI and data access, with dependencies pointing inward. Any package layout that respects it qualifies.
  - The codebase is small, and reviewers should find a feature in one place, named the way most Spring projects name things.
- **Decision:**
  - Package by feature (bounded context): `catalog`, `localpokemon`, `identity`, plus a small `shared` kernel (`exception`, `pagination`, `validation`, `config`).
  - Inside each feature, use the familiar Spring package names: `domain`, `service`, `client`, `repository`, `entity`, `controller`. The flow is controller → service → client/repository.
  - `ArchitectureTest` (ArchUnit) enforces:
    - the domain is framework-free (no Spring, JPA or Jackson);
    - services never depend on controllers, clients, repositories or entities;
    - controllers only call services;
    - clients and repositories never depend on controllers or services;
    - `shared` depends on no feature;
    - features have no cycles.
- **Consequences:**
  - Each feature is self-contained and reads like the business.
  - The domain stays unit-testable without Spring.
  - There's some mapping code (DTO ↔ domain ↔ entity). That's the price of keeping the domain independent.
- **Alternatives considered:**
  - **Hexagonal ports & adapters** packaged by technical layer (`domain`, `application/port/in|out`, `adapter/in|out`, `config`): valid Clean Architecture, but one feature ends up spread across many package trees, with jargon-heavy names and single-implementation use-case interfaces. That's ceremony for a project this size.
  - **Plain layered packages** (`controller/service/repository` at the top level): simpler, but features are scattered and the domain isn't protected.

### D5. Services as `@Service` beans; dependency inversion only for outbound I/O

- **Context:** Clean Architecture needs the business layer to stay independent of I/O. It doesn't need an interface on every boundary.
- **Decision:**
  - Services are ordinary `@Service` beans, and controllers call them directly. There are no inbound use-case interfaces.
  - Outbound I/O goes through a **domain interface** that the service depends on: `PokemonCatalog`, later `LocalPokemonRepository`. `client` (RestClient) or `repository` (Spring Data JPA) implements it.
- **Consequences:**
  - Fewer files. Services are still unit-tested by mocking the outbound interface.
  - The service layer depends on Spring stereotypes (`@Service`, later `@Transactional`). That's a deliberate trade-off for a small codebase.
- **Alternatives considered:**
  - Plain classes wired as `@Bean`s in a `config` package: keeps the service layer Spring-free, but adds wiring code with little benefit here.
  - Inbound use-case interfaces: decoupling with only one implementation, so no real benefit.

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
  - `@Cacheable` on the PokeAPI client methods, backed by Redis 8 with a 24 h TTL.
  - Cache per upstream resource (`pokemon`, `species`, `evolution-chain`, `pokemon-page`). The cached values are the trimmed upstream DTOs, stored as JSON with one typed serializer per cache, so no class names are stored in Redis.
  - 404s are not cached (`unless = "#result == null"`).
  - Short Redis timeouts (500 ms) plus a `LoggingCacheErrorHandler` mean a Redis outage only logs and falls back to PokeAPI.
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
  - Upstream errors map to domain exceptions (404 → not found; timeout, 5xx or a malformed response → 503).
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
  - PokeAPI client tests must cover mapping, 404, 5xx and timeouts.
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
  - **WebFlux / `WebClient` (reactive):** non-blocking, but `Mono`/`Flux` spreads through clients and tests and is harder to read and explain.
  - **A tuned platform thread pool (`ThreadPoolTaskExecutor`):** works, but it needs pool sizing and invites thread starvation under load.
  - **Sequential calls with caching only:** simplest, but the first load of every page would be slow.

### D20. Validation: domain guards for upstream data, Bean Validation for requests

- **Status:** Accepted. Reassessed when Phase 2 landed (see *Phase 2 outcome* below).
- **Context:**
  - Catalog domain records (`PokemonSummary`, `PokemonDetail`, `Ability`, `Stat`, `EvolutionStage`) and `Page`/`PageRequest` validate themselves in their constructors through `shared.validation.Require` (`text`, `positive`, `nonNegative`, `copy`).
  - The obvious alternative is Jakarta Bean Validation annotations (`@NotBlank`, `@Positive`, `@Min`/`@Max`).
  - Annotations only *declare* rules. A `Validator` must run them, and Spring only does that automatically at the **controller boundary**: built-in method validation for `@RequestParam`/`@PathVariable`, and `@Valid` for request bodies.
  - Our data reaches the domain in two different ways:
    - **From upstream:** `PokeApiPokemonCatalog` maps PokeAPI responses into domain records. No controller and no validator are involved.
    - **From users:** query parameters and path variables today (`page`, `size`, `idOrName`); JSON request bodies from Phase 2 (create, update, patch, sync).
- **Decision:** keep both mechanisms, each where it actually runs.
  - **Domain guards (`Require`, `PokemonKey`)** stay in constructors. They protect the domain whatever the entry point is, including upstream data and future sync jobs. The domain also stays framework-free, with no `jakarta.validation` dependency, and testable with plain JUnit.
  - **Bean Validation** is the tool for **request DTOs** from Phase 2: `@Valid` plus `@NotBlank`/`@Size`/`@Pattern`, reported as a 400 ProblemDetail with a `fieldErrors` extension.
  - **Query and path parameters** stay as they are. `PageRequest` already rejects a bad `page`/`size`, and `PokemonKey` normalizes and validates `idOrName`, both with a correct 400.
- **Why not switch the catalog records to annotations now:**
  - Nothing would invoke a validator on objects built from upstream data, so the annotations would be ignored.
  - User-facing behaviour is already correct (`size=500` → 400). Rewriting it would change how the check is written, not what anyone sees, at the cost of about 15 files and a dozen tests.
- **Known trade-off:**
  - If PokeAPI ever sent invalid data (a blank name, a negative weight), the domain guard throws `DomainValidationException`, which maps to **400**, although the fault is upstream. Ideally it would be a **503**.
  - It's very unlikely and the request fails either way, but it's the main reason to revisit this.
  - Possible fixes:
    - (a) make the catalog records plain mirrors of upstream data, without guards;
    - (b) translate guard failures inside the PokeAPI client into `MalformedPokeApiResponseException` (503).
- **Reassess in Phase 2 by answering:**
  1. Do request DTOs with Bean Validation make some domain guards redundant? Keep only rules that must hold however data arrives, e.g. "re-sync never overwrites proprietary fields" and "a stale `version` is a conflict".
  2. Should `page`/`size` move to `@PositiveOrZero`/`@Min`/`@Max` on the controller, for one consistent `fieldErrors` format across all 400s?
  3. Should upstream mapping failures become 503 (option a or b above)?
- **Alternatives considered:**
  - **Bean Validation everywhere, calling `Validator.validate(...)` manually in the client:** it works, but it adds a framework dependency to the domain path and ceremony for read-only data.
  - **Domain guards everywhere, with no Bean Validation:** request DTOs would lose standard, declarative, per-field error messages.
- **Phase 2 outcome (task 2.6):**
  - Request bodies (`ImportRequest`, `ProprietaryUpdateRequest`, `ProprietaryPatchRequest`, `SyncRequest`) use Bean Validation: `@NotBlank`, `@Size`, `@Pattern`, `@NotNull`/`@PositiveOrZero` on `version`. `GlobalExceptionHandler` reports the violations as 400 with a `fieldErrors` extension.
  - The `localpokemon` domain keeps only the rules that must hold whichever way data arrives (API or sync): at most 10 normalized tags, 1–50 distinct positive ids per sync batch, and the version check. The DTO limits mirror the domain constants (`ProprietaryData.MAX_TAGS`, `SyncBatch.MAX_IDS`), so the two never drift.
  - Questions 2 (`page`/`size` annotations) and 3 (upstream mapping failures → 503) stay open. Neither blocks anything.

### D21. MapStruct for response and entity → domain mappings

- **Status:** Accepted (task 6.6).
- **Context:**
  - Controllers turned domain objects into response records, and repositories turned entities into domain records, with hand-written positional constructor calls. `LocalPokemonResponse` alone has 17 arguments, 8 of them `String`.
  - Swapping two arguments of the same type still compiles. The tests only catch it for the fields they happen to assert.
- **Decision:** MapStruct `1.6.3` generates these mappings, matching fields by name:
  - **Controller responses:** `PokemonResponseMapper`, `LocalPokemonResponseMapper`, `AuthResponseMapper`.
  - **Entity → domain:** `UserEntityMapper`, `LocalPokemonEntityMapper`.
  - **Shared setup:** `shared.mapping.MappingConfig` sets Spring components, constructor injection and **`unmappedTargetPolicy = ERROR`**, so a new or renamed field without a source fails the build. `MeasureMappings` holds the named unit conversions (`kilograms`, `metres`).
  - Mappers live in the `controller` and `entity` packages. The domain stays framework-free, and `ArchitectureTest` is unchanged.
- **Kept hand-written, on purpose:**
  - **Entity writes** (`UserEntity.from`, `LocalPokemonEntity.copyFrom`). They set the `isNew` flag for assigned ids and update Hibernate's managed collections in place, so tags, types and abilities keep their identity under `@Version`. Generated setters would replace those collections and need public setters on the entities.
  - **`PokeApiMapper`.** It holds logic, not field copying: it orders types by slot, picks the English genus and latest flavor text, and falls back from artwork to sprite.
- **Consequences:**
  - Entities gained read-only getters, which MapStruct needs to read them. They still have no setters.
  - The generated `*MapperImpl` classes are excluded from the JaCoCo report and gate. Their defensive `null` branches are never reached by real data, and their correctness comes from the compile-time check plus the mapper, controller and repository tests.
- **Alternatives considered:**
  - **Keep hand-written constructors:** no dependency, but no compile-time check that each field lands in the right place.
  - **ModelMapper or another reflection-based mapper:** it fails at runtime instead of at compile time, and is slower.

