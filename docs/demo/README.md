# Demo recordings

Screen recordings of the main flows, made by the Playwright tests in [e2e/](../../e2e). Each file is one test, named `<spec>-<test-title>.webm`, and opens in any browser.

To refresh them, from the repo root:

```bash
docker compose up -d --build
cd e2e && npm run demo
```

What each flow covers is described in [E2E-TEST-PLAN.md](../E2E-TEST-PLAN.md).
