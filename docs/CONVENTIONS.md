# Conventions

## Commit convention

- **One task = one commit.** Tasks are the numbered items (`<phase>.<n>`) in [ROADMAP.md](ROADMAP.md).
- The commit message is the task id plus its short description, exactly as written in **bold** in the roadmap:
  ```
  0.1 monorepo layout
  1.4 pokeapi client adapter
  ```
  - Keep it lowercase, with no prefix or scope and no trailing period.
  - Put any extra detail in the commit body, after a blank line.
- Tests go in the same commit as the code they cover. Write them first (TDD).
- Tick the task's checkbox (`[ ]` → `[x]`) in `ROADMAP.md` as part of the same commit.
- If a task turns out to be too big, split it into `1.4a`, `1.4b`, … rather than renumbering.
- Each commit should build and pass its tests on its own (`./gradlew build` in `api/`, `npm test && npm run build` in `web/`).

## Code style

- Avoid comments. Write self-explanatory code instead.
- Prefer expressing intent through clear names (well-named methods, variables, and constants) and small, focused methods rather than a
  comment describing what the code does. If you feel the urge to write a comment, first try to refactor
  so the comment becomes unnecessary — extract a method whose name says it, or name a constant/variable
  after the concept.
- Only keep a comment when the code genuinely cannot carry the information on its own.
  e.g. the *why* behind a non-obvious business rule, a workaround for an external bug, or a link to a ticket.
  Never add comments that merely restate what the next line already says.

## Coding best practices

- **Enforce coding best practices**
  - Use meaningful variable and method names
  - Follow consistent indentation and formatting
  - Avoid the use of magic numbers and strings
  - Keep methods short and focused on a single responsibility
  - Use try-with-resources for resource management
  - Prefer immutability where possible
  - Use DRY principles
  - Use SOLID principles
  - Follow SonarQube for IDE guidelines
  - Never add logic to the controller layer, delegate to services
  - Always add {} for if clauses even if single-line
  - There's no need to return ResponseEntity (for recent Spring versions - which we're using) for the controllers unless when returning encoded data
  - Always remove unused imports
  - Always use static imports for constants and enums
  - Always create enums as dedicated files related to the respective domain, inside the enums folder, with a clear naming convention (e.g., EmployeeStatus.java)
  - Always add empty line at the end of files
