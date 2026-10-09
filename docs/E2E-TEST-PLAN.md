# End-to-end test plan

The browser-level tests for the Poke App. They run with [Playwright](https://playwright.dev) against the real stack, the same one `docker compose up --build` starts. The flows also produce the demo recordings kept in [docs/demo/](demo/).

## Why a browser layer

The other test layers already cover a lot:

- **API:** unit, slice, WireMock and Testcontainers tests, plus an end-to-end test inside Spring.
- **Web:** Vitest + Testing Library, with the API mocked by MSW.

None of them run the real browser against the real stack. These flows cover what only that combination shows:

- the nginx proxy (`/api` → `api:8080`) and the SPA fallback for deep links;
- the JWT stored in `localStorage` and attached to real API calls, plus the redirect to login and back;
- real PokeAPI data through the Redis cache;
- real 400/409 responses rendered in the forms;
- a clean browser console on every page the demo visits.

## Scope

| Spec | Story | Flow | What it checks |
|---|---|---|---|
| `smoke.spec.ts` | | The catalog loads; a protected page redirects to login | The stack is up and the proxy and routing work. Runs first. |
| `catalog.spec.ts` | US01, US02 | Browse page 1 → page 2 (`?page=2`), then open Eevee | Cards show sprite, category, weight and abilities. The detail page shows artwork, six stat bars, the description and the branching evolution chain. Clicking an evolution opens it. |
| `auth.spec.ts` | Users | Visit *My Pokedex* logged out → login as `ash` → back on *My Pokedex*; register a new user; log out | Public vs protected routes, the redirect to login and back, register-then-login, and the header showing the user |
| `pokedex.spec.ts` | US03 | Sync ids `1, 4, 7`; add Ditto from its detail page | The sync summary appears and the list updates. "Add to My Pokedex" creates the record, and a second add is refused. |
| `pokedex.spec.ts` | US04 | Edit Bulbasaur: invalid tag → inline error; valid tags and localized name → saved | Client-side validation, save, and the list showing the new data |
| `pokedex.spec.ts` | US04 | Stale edit: the record changes underneath an open form, then save | A 409 is shown with "Reload latest", and reloading recovers |
| `pokedex.spec.ts` | US04 | Delete a record with the in-page confirm | The record disappears, and `/api/v1/local-pokemon/{id}` returns 404 |

**Out of scope here:**
- Visual regression and cross-browser runs: Chromium only.
- Load testing.
- API edge cases already covered by `scripts/smoke-test.sh` and the API tests.

## Rules every flow follows

- **Console guard:** the `consoleGuard` fixture fails a test on any console error, warning or uncaught exception. The only allowed lines are Chrome's own network logs for the 401 and 409 responses the flows trigger on purpose. This is the browser counterpart of the Vitest setup, and it backs the "no warnings in the browser console" criterion.
- **Selectors:** accessible roles and labels (`getByRole`, `getByLabel`), the same ones a screen reader uses. No CSS classes and no test ids, so the flows also check accessibility.
- **Data:** the flows run against a shared database, one at a time (`workers: 1`), and leave it as they found it:
  - edits restore the original values through the API in `afterEach`;
  - records a flow creates, such as Ditto, are deleted at the end;
  - new users get a unique name per run;
  - syncing seeded ids only refreshes them, so it can run any number of times.

  The flows are safe to run against a stack you are using, and on a fresh `docker compose down -v` start.
- **Fast setup:** flows that aren't about logging in start already signed in. The `signIn` helper puts the session into `localStorage` before the page loads. Only `auth.spec.ts` goes through the login form.

## Running

```bash
docker compose up -d --build     # from the repo root: the stack under test
cd e2e
npm install
npx playwright install chromium  # once, downloads the browser
npm test                         # headless, about 30 s
npm run demo                     # visible browser, slowed down, records docs/demo/*.webm
npm run report                   # opens the last HTML report (traces and videos of failures)
```

`BASE_URL` points the tests at another stack (default `http://localhost:3000`).

## Demo mode and recordings

`npm run demo` (`DEMO=1`) changes four things:
- the browser is visible;
- every step is slowed by 600 ms;
- each test is recorded at 1280×800;
- the `demoRecording` fixture saves each video as `docs/demo/<spec>-<test-title>.webm`, replacing the previous take.

Re-record after any UI change, so the videos match the app.

## Status

| Step | Task | State |
|---|---|---|
| Playwright project, fixtures (console guard, API helper, sign-in, recording), smoke flows, this plan | 6.7 | Done |
| Catalog, auth and My Pokedex flows; recordings in `docs/demo/` | 6.8 | Next |
| Run the flows in CI against the compose stack | | To decide |
