# Poke App

A full-stack Pokemon application. It has a **Java / Spring Boot REST API** that integrates with [PokeAPI](https://pokeapi.co/docs/v2) and keeps a local relational replica that can be enriched with proprietary data, and a **React** web client that consumes the API.

> 🚧 Work in progress. See [docs/ROADMAP.md](docs/ROADMAP.md) for status.

## Purpose

The project shows how to build a robust backend service with **Clean Architecture** and **TDD**, plus a responsive, user-centric frontend on top of it. The service does three jobs:

- **Retrieves** Pokemon data from the public PokeAPI and caches it.
- **Replicates** selected Pokemon into a local PostgreSQL database.
- **Extends and modifies** those local records with proprietary fields that PokeAPI doesn't have: localized names, geographical metadata (region, habitat) and internal classification tags.

### User stories

| # | Story | What it delivers |
|---|---|---|
| **US01** | Pokemon enumeration | Browse Pokemon in paginated results. Each entry shows its sprite, category, weight and abilities. Responses are cached. |
| **US02** | Detailed view | Full data for a chosen Pokemon: image, base stats, description and evolution chain. |
| **US03** | Data synchronization | Persist Pokemon from PokeAPI into the local relational store, which then accepts proprietary fields. |
| **US04** | Local data modification | Update any locally stored Pokemon, with robust validation: 404 for missing records, 400 for malformed payloads, 409 for conflicts. |

### Technical goals

- **Clean Architecture:** the business layer is independent of both the web API and the data-access layer. An executable architecture test enforces this.
- **CRUD API:** full CRUD on the local dataset, using standard HTTP verbs and consistent response and error structures (RFC 9457 Problem Details).
- **Users:** user registration and authentication (JWT), with public and protected routes.
- **Data layer:** a dedicated data-access layer over PostgreSQL, with versioned migrations (Flyway).
- **Caching:** a caching layer for PokeAPI responses (Redis).
- **Testing:** thorough tests for every core component, written test-first.
- **Frontend:** a modern React SPA with clean component organization, efficient state management and no browser console warnings.
- **Ready to demo:** seeded demonstration data and credentials, and the whole stack runs in Docker.

## Tech stack

| | |
|---|---|
| **Backend** | Java 25 · Spring Boot 4.1 · Gradle (Kotlin DSL) · PostgreSQL 17 + Flyway · Redis 8 cache · JWT auth |
| **Frontend** | React 19 · TypeScript · Vite 8 · TanStack Query · Tailwind CSS 4 |
| **Testing** | JUnit 5 · Mockito · WireMock · Testcontainers · ArchUnit · JaCoCo · Vitest · Testing Library · MSW |

## Quick start

Prerequisites: Docker. For local development you also need JDK 25 and Node 24.

**IDE:** open the repository root (`poke-app/`) in IntelliJ IDEA. The root `settings.gradle.kts` includes `api/` as a composite build, so Gradle is imported automatically. Set Gradle JVM to 25 if prompted.

```bash
cp .env.example .env

# everything in Docker: web on http://localhost:3000, API on http://localhost:8080
docker compose up --build

# or run only the infrastructure and start the API and web locally
docker compose up -d postgres redis

# api
cd api && ./gradlew bootRun          # http://localhost:8080  (Swagger: /swagger-ui.html)

# web (another terminal)
cd web && npm install && npm run dev # http://localhost:5173
```

## Documentation

- [API README](api/README.md): setup, configuration and what each backend dependency is for
- [Web README](web/README.md): scripts, structure and what each frontend library is for
- [Architecture](docs/ARCHITECTURE.md): layers, data model, API contract, caching, auth, testing
- [Decision log](docs/DECISIONS.md): why each technical choice was made (ADR-style, D1–D20)
- [PokeAPI reference](docs/POKEAPI.md): upstream endpoints, mapping and quirks
- [Roadmap / next steps](docs/ROADMAP.md): numbered tasks, one commit each
- [Conventions](docs/CONVENTIONS.md): commit format, code style and best practices
- [CLAUDE.md](CLAUDE.md): working agreement for AI-assisted development

## Repository layout

```
api/    Spring Boot service (feature-first: catalog · localpokemon · identity, each with domain · service · client/repository · controller)
web/    React SPA
docs/   design docs
```
