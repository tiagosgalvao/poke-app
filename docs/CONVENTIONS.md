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
