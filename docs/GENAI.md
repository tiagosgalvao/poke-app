# Generative AI

This document has two parts:

1. **The exercise:** generating a RESTful task-management API with a GenAI coding tool. It covers the prompt, a representative sample of the output, and how the output was validated, corrected and hardened.
2. **How GenAI was used to build this repository,** and where human judgement had to step in.

The tool in both cases is **Claude Code** (agentic, runs in the terminal, reads and edits the repo, runs the build).

---

## Part 1: task-management API

### The requirement

- CRUD on tasks.
- Each task has a `title`, `description`, `status` and `due_date`.
- A task belongs to a user. A basic `User` model already exists.

### How I approach the prompt

A one-line prompt ("build a CRUD API for tasks in Spring Boot") gets a plausible demo: the entity is returned straight from the controller, `findById(id).get()` is used, `userId` comes from the request body, and there are no tests. Every gap I leave open is filled with the most common pattern in the training data, and that pattern is usually outdated or insecure. So the prompt does five things:

1. **Pins the stack and versions,** so the tool doesn't fall back on deprecated APIs such as `WebSecurityConfigurerAdapter` or `javax.*`.
2. **States the rules that are easy to get wrong:** ownership, validation, status codes and the error format.
3. **Asks for tests first,** and names the cases that must be covered.
4. **Says what not to do,** for example no Lombok, no entity in responses, and no `userId` in request bodies.
5. **Asks for a plan before code,** so I can correct the design while it is still cheap.

### The prompt

```text
You are working in an existing Spring Boot 4.1 / Java 25 / Gradle (Kotlin DSL) service
with PostgreSQL + Flyway and Spring Security (JWT resource server, HS256).
A `users` table and a `User` entity (UUID id, username, email) already exist.
The authenticated user's id is in the JWT claim `uid`.

Goal: add a REST API for a simple task-management system.

Domain
- Task: id (UUID), title, description, status, due_date, owner (User), version,
  created_at, updated_at.
- status is an enum: TODO, IN_PROGRESS, DONE. New tasks default to TODO.
- title: required, trimmed, 1–120 chars. description: optional, up to 2000 chars.
- due_date: a calendar date (no time, no zone). On create it must be today or later
  (today = server Clock, injected so tests can fix it). Updating an overdue task
  without changing its due_date must still be allowed.

API (base path /api/v1/tasks, JSON in snake_case so the field is `due_date`)
- GET    /            current user's tasks, paginated (page, size ≤ 50), optional ?status=
- GET    /{id}
- POST   /            201 + Location header
- PUT    /{id}        full update, requires the current `version` (optimistic locking)
- DELETE /{id}        204
- Every endpoint requires authentication. A user only ever sees their own tasks:
  someone else's task id returns 404, never 403, so ids can't be probed.
- The owner always comes from the token. Request bodies must not accept an owner or user id.

Errors: RFC 9457 ProblemDetail everywhere.
- 400 for bean-validation failures, malformed JSON, unknown enum values and bad UUIDs,
  with a `fieldErrors` array of {field, message}.
- 401 without a valid token, 404 for missing or foreign tasks, 409 for a stale version.
- 500 must not leak exception messages.

Architecture and style
- Layers: controller (request/response records only) → service (business rules,
  @Transactional) → repository (Spring Data JPA). Never return the entity from the controller.
- Flyway migration V{n}__tasks.sql with FK to users (on delete cascade), check
  constraints for status and title length, an index on (owner_id, due_date).
- Records for DTOs, constructor injection, no Lombok, no field injection.

Tests: write them first.
- Service unit tests (Mockito, fixed Clock): create defaults to TODO, a past due_date is rejected,
  an overdue task can still be updated, a stale version → conflict, a foreign task → not found.
- @WebMvcTest for the controller: 201 + Location, 400 with fieldErrors (blank title,
  unknown status, malformed JSON), 401 without a token, 404 for a foreign task, 204 on delete.
- @DataJpaTest with Testcontainers Postgres for the repository query scoped by owner.

Process: first reply with a short plan (files and their responsibilities, and any assumption
you're making). Wait for my OK. Then implement it test-first, run `./gradlew build`, and
show me the failing → passing tests.
```

### Representative output (after review)

The generated plan was accepted with two changes, described in [What I corrected](#what-i-corrected). This is the core of the result.

**Migration**

```sql
create table task (
    id          uuid primary key,
    owner_id    uuid         not null references users (id) on delete cascade,
    title       varchar(120) not null check (length(trim(title)) > 0),
    description varchar(2000),
    status      varchar(16)  not null check (status in ('TODO', 'IN_PROGRESS', 'DONE')),
    due_date    date         not null,
    version     bigint       not null default 0,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null
);
create index task_owner_due_idx on task (owner_id, due_date);
```

**Request and response records.** There is no owner field in the request, and the response never exposes the entity.

```java
public record TaskRequest(
		@NotBlank @Size(max = Task.TITLE_MAX) String title,
		@Size(max = Task.DESCRIPTION_MAX) String description,
		TaskStatus status,
		@NotNull LocalDate dueDate,
		Long version) {
}

public record TaskResponse(UUID id, String title, String description, TaskStatus status,
		LocalDate dueDate, boolean overdue, long version, Instant updatedAt) {

	static TaskResponse from(Task task, LocalDate today) {
		return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
				task.getDueDate(), task.isOverdue(today), task.getVersion(), task.getUpdatedAt());
	}
}
```

`spring.jackson.property-naming-strategy: SNAKE_CASE` makes the JSON field `due_date` while the Java name stays idiomatic.

**Service.** The business rules live here, ownership is part of every query, and the clock is injected.

```java
@Service
@Transactional
public class TaskService {

	private final TaskRepository tasks;
	private final Clock clock;

	public TaskService(TaskRepository tasks, Clock clock) {
		this.tasks = tasks;
		this.clock = clock;
	}

	public Task create(UUID ownerId, TaskRequest request) {
		requireNotInThePast(request.dueDate());
		var status = request.status() == null ? TaskStatus.TODO : request.status();
		return tasks.save(Task.create(ownerId, request.title().strip(), request.description(), status,
				request.dueDate(), clock.instant()));
	}

	@Transactional(readOnly = true)
	public Task get(UUID ownerId, UUID id) {
		return tasks.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new TaskNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Page<Task> list(UUID ownerId, TaskStatus status, Pageable pageable) {
		return status == null
				? tasks.findByOwnerId(ownerId, pageable)
				: tasks.findByOwnerIdAndStatus(ownerId, status, pageable);
	}

	public Task update(UUID ownerId, UUID id, TaskRequest request) {
		var task = get(ownerId, id);
		if (request.version() == null || request.version() != task.getVersion()) {
			throw new StaleTaskException(id);
		}
		if (!request.dueDate().equals(task.getDueDate())) {
			requireNotInThePast(request.dueDate());
		}
		task.update(request.title().strip(), request.description(), request.status(), request.dueDate(),
				clock.instant());
		return task;
	}

	public void delete(UUID ownerId, UUID id) {
		tasks.delete(get(ownerId, id));
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

	private void requireNotInThePast(LocalDate dueDate) {
		if (dueDate.isBefore(today())) {
			throw new InvalidTaskException("dueDate", "must be today or later");
		}
	}
}
```

**Controller.** It is thin: it reads the owner from the token and maps the result to the response record.

```java
@RestController
@RequestMapping("/api/v1/tasks")
class TaskController {

	private static final String OWNER_CLAIM = "uid";

	private final TaskService service;

	TaskController(TaskService service) {
		this.service = service;
	}

	@GetMapping
	PageResponse<TaskResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) TaskStatus status,
			@PageableDefault(size = 20) @MaxPageSize(50) Pageable pageable) {
		var today = service.today();
		return PageResponse.from(service.list(owner(jwt), status, pageable).map(t -> TaskResponse.from(t, today)));
	}

	@GetMapping("/{id}")
	TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return TaskResponse.from(service.get(owner(jwt), id), service.today());
	}

	@PostMapping
	ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TaskRequest request) {
		var task = service.create(owner(jwt), request);
		return ResponseEntity.created(URI.create("/api/v1/tasks/" + task.getId()))
				.body(TaskResponse.from(task, service.today()));
	}

	@PutMapping("/{id}")
	TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody TaskRequest request) {
		return TaskResponse.from(service.update(owner(jwt), id, request), service.today());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		service.delete(owner(jwt), id);
	}

	private static UUID owner(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString(OWNER_CLAIM));
	}
}
```

**Tests (excerpt).** The failing tests were written first and drive the rules above.

```java
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	private static final LocalDate TODAY = LocalDate.parse("2026-03-10");
	private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
	private static final UUID ALICE = UUID.randomUUID();
	private static final UUID BOB = UUID.randomUUID();

	@Mock TaskRepository tasks;
	TaskService service;

	@BeforeEach
	void setUp() {
		service = new TaskService(tasks, CLOCK);
	}

	@Test
	void rejectsADueDateInThePastOnCreate() {
		var request = new TaskRequest("Pay rent", null, null, TODAY.minusDays(1), null);

		assertThatThrownBy(() -> service.create(ALICE, request))
				.isInstanceOf(InvalidTaskException.class)
				.hasMessageContaining("dueDate");
	}

	@Test
	void anOverdueTaskCanStillBeUpdatedWithoutMovingItsDueDate() {
		var overdue = Task.create(ALICE, "Pay rent", null, TaskStatus.TODO, TODAY.minusDays(3), CLOCK.instant());
		given(tasks.findByIdAndOwnerId(overdue.getId(), ALICE)).willReturn(Optional.of(overdue));

		var updated = service.update(ALICE, overdue.getId(),
				new TaskRequest("Pay rent", null, TaskStatus.DONE, TODAY.minusDays(3), 0L));

		assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
	}

	@Test
	void anotherUsersTaskIsNotFound() {
		var id = UUID.randomUUID();
		given(tasks.findByIdAndOwnerId(id, BOB)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(BOB, id)).isInstanceOf(TaskNotFoundException.class);
	}
}
```

```java
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TaskControllerTest {

	@Autowired MockMvc mvc;
	@MockitoBean TaskService service;

	@Test
	void rejectsAnUnknownStatusWith400() throws Exception {
		mvc.perform(post("/api/v1/tasks").with(jwt().jwt(j -> j.claim("uid", ALICE.toString())))
						.contentType(APPLICATION_JSON)
						.content("""
								{"title": "Pay rent", "status": "SOMEDAY", "due_date": "2026-03-12"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void requiresAToken() throws Exception {
		mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
	}
}
```

### How I validated the suggestions

I treat AI output like a pull request from a fast, confident colleague who has never seen the codebase. It is useful, but nothing merges until it is verified.

1. **Plan review before code.** The plan is where design mistakes are cheapest. Two were caught there, both described in the next section: the owner came from the body, and the due date rule applied to every update.
2. **Read every line.** I don't skim a diff because the tests are green. I check imports (Jakarta vs `javax`, Spring Security 7 APIs), transaction boundaries and what is serialised.
3. **The build is the referee.** `./gradlew build` runs the unit, slice and Testcontainers tests. A suggestion that "should work" but has no test proving it doesn't count.
4. **Check that the tests can fail.** For each key rule, I break the code on purpose (remove the owner filter, drop `@Valid`) and confirm a test goes red. The first generated tests included some that only asserted what the mocks returned, and those were rewritten.
5. **Try to break it by hand.** With the app running, I send requests designed to break it:
   - another user's id;
   - a token for a deleted user;
   - `"status": "done"` (lowercase), `"due_date": "10/03/2026"`, a 10 KB title, `null` body fields and an extra `"owner_id"`;
   - two PUTs with the same `version`.

   Then I check each status code and error body.
6. **Check the docs, not my memory.** When the tool and I disagree about a framework detail, the reference documentation decides. That applies, for example, to how Spring Boot 4 maps an unknown enum value, or to Jackson 3 naming strategies.

### What I corrected

| What the first draft did | Why it was wrong | What changed |
|---|---|---|
| Accepted `userId` in `TaskRequest` | Anyone could create or move tasks into another user's account (mass assignment) | The owner always comes from the JWT `uid` claim. Unknown JSON properties are ignored, and a test sends `owner_id` and checks it has no effect |
| `findById(id)` and then checked the owner in the controller, returning 403 | Easy to forget on one endpoint. A 403 also confirms the id exists | `findByIdAndOwnerId` in every path, and foreign ids return 404 |
| `@FutureOrPresent` on the request for both create and update | Overdue tasks could never be edited or completed. Bean Validation also used the system clock, not the injected one | The rule moved to the service: it applies on create, and on update only when the date changes. Tests use a fixed `Clock` |
| Returned the `Task` entity, with its `User`, from the controller | Leaks the password hash, triggers lazy-loading errors and couples the API to the schema | Response records, plus a computed `overdue` flag |
| `status` was a free `String` | Any value was stored. A typo became data | An enum with a DB check constraint. Unknown values return 400 |
| `Optional.get()` and a generic `RuntimeException` | A missing task returned 500 with a stack trace message | Domain exceptions mapped to ProblemDetail (404, 409). A 500 has a generic body |
| `findAll()` with no paging | Unbounded responses | `Pageable`, with page size capped at 50 |
| Last write wins | Two tabs silently overwrote each other | A `version` column, with stale updates returning 409 |
| Some tests only verified that mocks were called | They passed even with the logic deleted | Rewritten to assert behaviour, with a mutation check |

### Edge cases, authentication and validation

- **Authentication:** every task route requires a valid JWT (`401` ProblemDetail otherwise). There is no anonymous read.
- **Authorisation:** row-level ownership is enforced in the repository query, so it can't be bypassed by a new endpoint that forgets a check. Foreign ids return 404.
- **Validation layers:**
  - Bean Validation on the request handles shape and lengths.
  - The service handles business rules such as due dates and versions.
  - Database constraints are the last line of defence.

  Each error type maps to one status code.
- **Dates:** `due_date` is a `LocalDate`, so there is no time-zone drift. "Today" comes from an injected `Clock`, which keeps tests deterministic. A task is "overdue" when it is not `DONE` and its date has passed. That value is computed, not stored.
- **Whitespace and blanks:** titles are trimmed. `"   "` is rejected both by `@NotBlank` and by the DB check.
- **Concurrency:** optimistic locking through `version`, so the client must send what it read.
- **Deleting a user** cascades to their tasks, so there are no orphans.

---

## Part 2: how GenAI was used to build this repository

The whole project was built with Claude Code as a pair programmer. The aim was speed without giving up control, so the setup is designed to keep the AI on rails.

### Guardrails

- **[CLAUDE.md](../CLAUDE.md)** is the standing brief. It covers the stack, the architecture rules, the conventions, the commands, and "don't commit unless asked". Every session starts from it, so I don't re-explain the project or let it drift.
- **[ROADMAP.md](ROADMAP.md)** breaks the work into small numbered tasks, one commit each. Each prompt is one task, which keeps diffs reviewable and makes it obvious when the tool wanders out of scope.
- **[DECISIONS.md](DECISIONS.md)** records every choice and why. When the AI proposes something that contradicts a decision, the log wins, or the decision is revisited on purpose.
- **Executable rules instead of trust:**
  - `ArchitectureTest` (ArchUnit) fails the build if a layer rule is broken.
  - JaCoCo fails it below 95% line and 90% branch coverage.
  - The web test setup fails any test that logs a console error or warning.
  - Testcontainers runs the real Postgres and Redis, so persistence code can't pass against a fake.
- **TDD as the interface:** I ask for the failing test first, review it as the specification, then ask for the code. A wrong test is far easier to spot than wrong code.

### Where the AI got it wrong and I stepped in

These are real corrections from this repository's history:

- **Architecture vocabulary:** the first structure used hexagonal "ports and adapters" packages. For a service this size that was ceremony, and the term confused readers. I switched to feature-first packages with plain Spring layers, keeping dependency inversion only for outbound I/O ([D4](DECISIONS.md#d4-feature-first-packages-with-clean-layers-inside), [D5](DECISIONS.md#d5-services-as-service-beans-dependency-inversion-only-for-outbound-io)).
- **Naming:** a feature was first called `pokedex`, which was ambiguous between the upstream catalogue and the local copy. It was renamed to `localpokemon`.
- **Spring wiring order:** a `@Service` was introduced in a task before the bean it needed existed, so the context failed to start. The fix was to reorder the roadmap so implementations land before the services that use them.
- **JPA with assigned ids:** new entities were saved through `merge` and started at `version = 1`. The fix was `Persistable` with an `isNew` flag, plus a repository test asserting that new rows start at version 0.
- **Leaking 500s:** `ObjectOptimisticLockingFailureException` escaped as a 500. It is now translated to a domain `StaleVersionException`, which returns 409, with a concurrency test.
- **Integration tests:** a class-level `@Transactional` in a migration test caused Postgres "current transaction is aborted" errors after an expected constraint violation. The fix was explicit cleanup in `@AfterEach` instead.
- **React tests:** session resets in `afterEach` produced `act()` warnings. The stricter console guard caught them, and the fix was to unmount before resetting the store.
- **Avoiding over-engineering:** reusing the domain `Require` guards for request bodies too was considered and rejected. Bean Validation stays at the edge and `Require` guards domain invariants only ([D20](DECISIONS.md#d20-validation-domain-guards-for-upstream-data-bean-validation-for-requests)).

### What I would tell a team

- Give the tool the same context a new joiner gets: a written brief, conventions and a definition of done. Prompts get shorter and output gets better.
- Make the rules executable, through architecture tests, coverage gates and strict linters. Then the AI's mistakes fail the build instead of failing review.
- Keep tasks small. Review the plan, then the tests, then the code.
- Use the tool for its strengths: boilerplate, test cases you didn't think of, reading unfamiliar APIs and refactors across many files. Keep a human on security boundaries, data modelling and trade-offs.
