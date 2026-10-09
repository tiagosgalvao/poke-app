# Poke App: API

A Spring Boot REST API that integrates with PokeAPI, keeps a local PostgreSQL replica with proprietary fields, and exposes catalog, local CRUD and auth endpoints. The design is described in [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md).

## Requirements

- JDK 25. Gradle picks it up through the toolchain, and the wrapper downloads Gradle itself.
- Docker, for Postgres and Redis in local runs and for Testcontainers in tests.

## Commands

```bash
docker compose up -d postgres redis   # from the repo root
./gradlew bootRun                     # http://localhost:8080 · Swagger UI: /swagger-ui.html · health: /actuator/health
./gradlew build                       # compile + all tests + JaCoCo report
./gradlew test --tests '*ArchitectureTest'
```

The coverage report is written to `build/reports/jacoco/test/html/index.html`. `./gradlew build` also enforces a minimum of 95% line and 90% branch coverage.

## Docker image

```bash
docker build -t poke-app-api .        # from api/
```

- **Build stage:** `eclipse-temurin:25-jdk` builds `poke-api.jar`, with a BuildKit cache mount for `~/.gradle`, then extracts Spring Boot's layers (dependencies, loader, snapshot dependencies, application). Code changes then only rebuild the small top layer.
- **Run stage:** `eclipse-temurin:25-jre`, running as the non-root user `poke`, with `JarLauncher`.
- **Healthcheck:** calls `/actuator/health` through bash's `/dev/tcp`, so no curl or wget is needed in the image.

## Configuration

Every setting comes from an environment variable, with a local default in `src/main/resources/application.yml`:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/poke` / `poke` / `poke` | PostgreSQL connection |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis cache |
| `REDIS_TIMEOUT` / `REDIS_CONNECT_TIMEOUT` | `500ms` / `500ms` | Kept short so a Redis outage falls back to PokeAPI quickly |
| `CACHE_TTL` | `24h` | Time-to-live for cached PokeAPI data |
| `POKEAPI_BASE_URL` | `https://pokeapi.co/api/v2` | Upstream API (tests point it at WireMock) |
| `POKEAPI_CONNECT_TIMEOUT` / `POKEAPI_READ_TIMEOUT` | `2s` / `5s` | Upstream timeouts; exceeding them returns 503 |
| `JWT_SECRET` | dev-only value | HS256 signing key, at least 32 bytes |
| `JWT_TTL` | `2h` | Access token lifetime |

## Package layout

Feature-first, with the usual Spring layers inside each feature (see [D4](../docs/DECISIONS.md#d4-feature-first-packages-with-clean-layers-inside)):

```
com.poke
├── shared/
│   ├── exception/      DomainException hierarchy; handler/GlobalExceptionHandler
│   ├── pagination/     Page, PageRequest, PageResponse
│   ├── measure/        Measures (unit conversion)
│   ├── mapping/        MappingConfig, MeasureMappings (MapStruct setup)
│   ├── validation/     Require
│   └── config/         SecurityConfig
├── catalog/            US01–US02
│   ├── domain/         PokemonSummary, PokemonDetail, EvolutionStage, PokemonKey, PokemonCatalog (interface)
│   ├── service/        CatalogService
│   ├── client/         PokeApiClient, PokeApiPokemonCatalog, mapper, cache config, dto/, enums/
│   └── controller/     PokemonController, PokemonResponses
├── localpokemon/       US03–US04: domain, service, repository, entity, controller
└── identity/           users and auth: domain, service, security, repository, entity, controller
```

`ArchitectureTest` enforces the dependency rule: `controller → service → domain ← client / repository`. The domain stays framework-free, and features have no cycles between them.

## Dependencies and why they are here

Versions are managed by the Spring Boot BOM (Boot **4.1.1**) unless pinned in `build.gradle.kts`.

### Runtime

| Dependency | What it does here |
|---|---|
| **spring-boot-starter-webmvc** | The REST layer: `@RestController`, JSON serialization (Jackson), embedded Tomcat, and `RestClient`, the HTTP client used to call PokeAPI. Virtual threads are enabled (`spring.threads.virtual.enabled`), so blocking calls stay cheap and the PokeAPI fan-out can run concurrently (see [D19](../docs/DECISIONS.md#d19-virtual-threads-enabled-globally)). |
| **spring-boot-starter-restclient** | Auto-configures the `RestClient.Builder` used by the PokeAPI client, with Boot's Jackson setup and HTTP observability. Boot 4 ships it as a separate module. |
| **spring-boot-starter-validation** | Bean Validation (Hibernate Validator). `@NotBlank`, `@Size`, `@Pattern` and similar annotations on request DTOs reject malformed payloads with **400** before they reach a service. |
| **spring-boot-starter-data-jpa** | Hibernate + Spring Data JPA, used only inside `<feature>.repository` and `<feature>.entity`. It provides entities, repositories, optimistic locking (`@Version` → **409**) and paging. |
| **postgresql** (runtime) | JDBC driver for PostgreSQL, the system's source of truth. |
| **spring-boot-starter-flyway** + **flyway-database-postgresql** | Versioned SQL migrations (`src/main/resources/db/migration/V{n}__*.sql`) for both schema and seed data. Hibernate only *validates* the schema (`ddl-auto: validate`) and never changes it. |
| **spring-boot-starter-cache** | The Spring Cache abstraction (`@Cacheable`), applied on `PokeApiClient` in `catalog.client`, so the caching policy stays out of the business layer. |
| **spring-boot-starter-data-redis** | Redis as the cache store: shared across instances and inspectable during the demo (`redis-cli KEYS '*'`). A `LoggingCacheErrorHandler` plus 500 ms Redis timeouts make the API fall back to PokeAPI if Redis is down. |
| **spring-boot-starter-security** | Authentication and authorization: a stateless filter chain, public vs protected routes, and BCrypt password hashing. |
| **spring-boot-starter-security-oauth2-resource-server** | Validates `Authorization: Bearer <JWT>` tokens (HS256 signature with the algorithm pinned, and expiry; see [ARCHITECTURE §8](../docs/ARCHITECTURE.md#how-a-request-with-a-bearer-token-is-checked)) with Spring's built-in support, so there's no hand-written JWT filter. It also brings Nimbus JOSE, which is used to *issue* tokens at login. |
| **spring-boot-starter-actuator** | `/actuator/health` (with liveness and readiness probes), used by the Docker healthcheck. Only `health` and `info` are exposed. |
| **springdoc-openapi-starter-webmvc-ui** `3.1.1` | Generates the OpenAPI spec from the controllers and serves **Swagger UI** at `/swagger-ui.html`, which is used for the demo and for manual testing. |
| **mapstruct** + **mapstruct-processor** `1.6.3` (annotation processor) | Generates the domain → response and entity → domain mappings at compile time. Fields are matched by name, and an unmapped target fails the build (`MappingConfig`), so mixing up two `String` arguments can no longer compile silently. See [D21](../docs/DECISIONS.md#d21-mapstruct-for-response-and-entity--domain-mappings). |
| **spring-boot-configuration-processor** (annotation processor) | Generates metadata for the `poke.*` properties, so the IDE autocompletes and validates them in `application.yml`. |

### Test

| Dependency | What it does here |
|---|---|
| **spring-boot-starter-\*-test** (webmvc, data-jpa, security, …) | JUnit 5, AssertJ, Mockito and Spring test slices: `@WebMvcTest` for controllers, `@DataJpaTest` for repositories, plus security test helpers that let tests act as an authenticated user. |
| **spring-boot-testcontainers** + **testcontainers-junit-jupiter** + **testcontainers-postgresql** | Starts **real** Postgres 17 and Redis 8 containers for integration tests (`TestcontainersConfiguration`). `@ServiceConnection` wires them in automatically, so tests run against the same engines as production, not H2. `TestPokeApiApplication` reuses the same setup to run the app locally with no compose. |
| **archunit-junit5** `1.5.0` | `ArchitectureTest` turns the Clean Architecture rules into failing tests: no Spring, JPA or Jackson in any `domain`, inward-only dependencies, controllers never touching clients or repositories, and no cycles between features. |
| **wiremock-standalone** `3.13.2` | A fake PokeAPI over real HTTP, using recorded JSON fixtures. It tests mapping, 404, 5xx and timeouts deterministically and without network access. |
| **junit-platform-launcher** | Required by Gradle to run the JUnit Platform. |

### Build plugins

| Plugin | Purpose |
|---|---|
| `org.springframework.boot` | `bootRun` and `bootJar`: a layered executable jar for the Docker image |
| `io.spring.dependency-management` | Applies the Spring Boot BOM, so starters need no explicit versions |
| `jacoco` | Code-coverage report, generated after every `test` run |

### Deliberately not used

- **Lombok:** Java records cover DTOs and value objects, which is one less annotation processor to explain.
- **H2:** tests use real Postgres through Testcontainers, so SQL and migrations behave as they do in production.
- **WebFlux / reactive:** `RestClient` on virtual threads gives concurrency for the PokeAPI fan-out with plain, readable code.
- **A separate JWT library (jjwt):** Spring Security's resource server and Nimbus already cover issuing and validating tokens.
