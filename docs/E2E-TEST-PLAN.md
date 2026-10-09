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
| `smoke.spec.ts` | | The catalog loads with no *My Pokedex* link for a visitor; a protected page redirects to login | The stack is up and the proxy and routing work. Runs first. |
| `catalog.spec.ts` | US01, US02 | Browse page 1 → page 2 (`?page=2`), then open Eevee | Cards show sprite, category, weight and abilities. The detail page shows artwork, six stat bars, the description and the branching evolution chain. Clicking an evolution opens it. |
| `auth.spec.ts` | Users | Open `/my-pokedex` logged out → login as `ash` → back on *My Pokedex*, with the link now in the header; log out, landing on the catalog with the link gone; a wrong password is refused; register a new user | Public vs protected routes, the redirect to login and back, register-then-login, and the header showing the user |
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
npm test                         # the 11 regression checks, headless, a few seconds
npm run demo                     # needs ffmpeg: the 3 narrated chapters in a visible browser, recorded into docs/demo/ (about 4 min)
npm run report                   # opens the last HTML report (traces and videos of failures)
```

`BASE_URL` points the tests at another stack (default `http://localhost:3000`).

In CI, `.github/workflows/e2e.yml` does the same on a GitHub runner: it starts the stack with `docker compose up --build --wait`, runs `npm test` (the `checks` project, with one retry), and uploads the HTML report, traces and service logs if anything fails. The narrated `demo` project never runs in CI.

## Demo chapters and recordings

The demo is a separate Playwright project (`demo`, in `e2e/demo/`), apart from the regression checks (`checks`, in `e2e/tests/`). It replays the demo script as three narrated chapters:

| Chapter | Covers |
|---|---|
| `01-catalog-and-details` | US01 catalog cards and pagination in the URL; US02 detail view: artwork, stats, description, Eevee's branching evolution chain |
| `02-accounts-and-protected-routes` | Visitor menu, redirect to login and back, a refused wrong password, log in and log out, registering a new account |
| `03-my-pokedex` | US03 sync and adding from the detail page; US04 edit with validation, a stale-edit 409 with "Reload latest", delete with an in-page confirm |

How the narration works:
- **`e2e/demo/narrator.ts`** injects an overlay into every page with three parts: a speech balloon, an outline around the element in use, and a cursor dot that glides to each click.
- **Balloons** sit beside the target when there's room, otherwise below or above it. They stay up long enough to read: at least 2.5 s, or 300 ms per word.
- **Typing** is shown key by key.
- **The overlay** never takes clicks and never logs to the console, so the console guard still applies.

The chapters keep their assertions, so a broken flow fails the recording instead of producing a misleading video. They also restore the data they change.

`npm run demo` opens a visible browser and records at 1280×800, holding the final screen for 2 s. Playwright records WebM; the fixture converts each chapter with ffmpeg to `docs/demo/<chapter>.mp4` (H.264, `yuv420p`, `faststart`), which plays in QuickTime, browsers and GitHub. ffmpeg must be installed (`brew install ffmpeg`), or set `FFMPEG` to its path. It takes about 4 minutes. Re-record after any UI change, so the videos match the app.

## Status

| Step | Task | State |
|---|---|---|
| Playwright project, fixtures (console guard, API helper, sign-in, recording), smoke flows, this plan | 6.7 | Done |
| Hide the *My Pokedex* link from visitors (app change before recording) | 6.8 | Done |
| Fix: logging out on a protected page landed on login instead of home (found by `auth.spec.ts`) | 6.9 | Done |
| Catalog, auth and My Pokedex flows (11 tests), plus three narrated demo chapters (balloons, highlight, cursor) recorded as MP4 in `docs/demo/` | 6.10 | Done |
| Run the checks in CI against the compose stack (`.github/workflows/e2e.yml`) | 6.11 | Done |
