# Poke App: Web

A React single-page app that consumes the Poke App API. It lets you browse and view Pokemon, log in, and manage the local Pokedex (sync, edit and delete). The design is described in [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md#9-frontend-architecture).

## Requirements

- Node 24
- The API running on `http://localhost:8080`. The dev server proxies `/api` to it, so no CORS setup is needed.

## Scripts

```bash
npm install
npm run dev        # http://localhost:5173
npm test           # vitest run (one-off)
npm run test:watch # vitest in watch mode
npm run lint       # oxlint
npm run typecheck  # tsc -b
npm run build      # type-check + production build into dist/
npm run preview    # serve dist/ locally
```

## Structure

```
src/
├── api/            typed fetch client: base URL, bearer header, ProblemDetail errors, 401 → logout
├── features/
│   ├── catalog/    US01 list + US02 detail (query hooks + components)
│   ├── local-pokemon/   US03 sync + US04 edit/delete ("My Pokedex")
│   └── auth/       login/register forms, auth store, route guard
├── components/     shared UI (pagination, stat bar, skeletons, error state…)
├── routes/         router definition + layouts
└── test/           Vitest setup, MSW handlers
```

Folders are created as each feature lands (see [../docs/ROADMAP.md](../docs/ROADMAP.md)).

## Dependencies and why they are here

### Runtime

| Library | Version | What it does here |
|---|---|---|
| **react** / **react-dom** | 19.3 | The UI library and its DOM renderer. The app runs in `StrictMode`, which surfaces unsafe patterns and duplicate effects during development. |
| **react-router** | 8.4 | Client-side routing (`/`, `/pokemon/:id`, `/login`, `/register`, `/my-pokedex`). Uses the data router (`createBrowserRouter`) with nested layouts. A `RequireAuth` wrapper protects private routes. |
| **@tanstack/react-query** | 5.104 | **Server state.** It fetches, caches and deduplicates API calls, tracks loading and error states, keeps the previous page during pagination, and invalidates and refetches after mutations (sync, edit, delete). API data never goes into a global store by hand. |
| **zustand** | 5.0 | **Client state**, specifically auth: token, current user, login and logout. It's a tiny hook-based store with a `persist` middleware (`localStorage`), so the session survives reloads without Context-provider boilerplate. |
| **react-hook-form** (RHF) | 7.89 | **Forms** (login, register, edit Pokemon, sync dialog). Inputs are uncontrolled, so there are few re-renders. It handles field registration, dirty and touched state, submit handling and error display. It can also map server-side 400 `fieldErrors` back onto the fields (`setError`). |
| **zod** | 4.6 | **Schema validation and types.** Each form has a zod schema that mirrors the API rules (lengths, required fields, allowed characters, tag limits), and `z.infer` derives the TypeScript type from it, so validation and types never drift. It can also parse API responses at the boundary. |
| **@hookform/resolvers** | 5.9 | The bridge between RHF and zod: `zodResolver(schema)` makes RHF validate with the zod schema. |

### Build and styling

| Library | Version | What it does here |
|---|---|---|
| **vite** | 8.3 | Dev server with hot module reload and the production bundler. Its `/api` proxy forwards to Spring Boot in development. |
| **@vitejs/plugin-react** | 6.1 | JSX transform and React Fast Refresh for Vite |
| **typescript** | 6.0 | Static typing. `tsc -b` runs as part of `build`, so type errors fail the build. |
| **tailwindcss** + **@tailwindcss/vite** | 4.3 | Utility-first CSS through the Vite plugin, with no PostCSS config. Mobile-first responsive layouts (`sm:`, `md:`, `lg:`) meet the "responsive, user-centric design" requirement without writing custom CSS files. |
| **oxlint** | 1.85 | A fast linter (Rust) with React and TypeScript rules, including `rules-of-hooks` and `only-export-components`. It helps keep the console free of warnings. |
| **@types/node**, **@types/react**, **@types/react-dom** | — | Type definitions for the toolchain and for React |

### Testing

| Library | Version | What it does here |
|---|---|---|
| **vitest** | 5.0 | Test runner. It shares Vite's config and transforms, so there's no separate Jest/Babel setup. Configured in `vite.config.ts` with the `jsdom` environment and globals. |
| **jsdom** | 30.1 | A browser-like DOM in Node, so components render in tests |
| **@testing-library/react** | 16.3 | Renders components and queries them the way a user would (by role, label or text) rather than by implementation details |
| **@testing-library/user-event** | 14.6 | Realistic user interactions (typing, clicking, tabbing) for form and dialog tests |
| **@testing-library/jest-dom** | 7.0 | Readable DOM assertions such as `toBeInTheDocument()`, `toHaveValue()` and `toBeDisabled()`. Loaded in `src/test/setup.ts`. |
| **msw** (Mock Service Worker) | 2.15 | Mocks the API at the **network level**. Components use the real fetch client and React Query, and MSW answers the requests, including 400, 401, 404 and 409 ProblemDetail errors. Tests therefore cover the whole data flow without a running backend. |

### Why this split of state

| Kind of state | Where it lives |
|---|---|
| Data that belongs to the server (Pokemon, local records) | React Query cache |
| Who is logged in | Zustand |
| What the user is typing | react-hook-form |
| What page or filter is shown | The URL (React Router search params), so it's shareable and back-button friendly |

Each piece of state has exactly one owner, which keeps components simple and avoids sync bugs.
