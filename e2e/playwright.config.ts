import { defineConfig, devices } from '@playwright/test'

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000'
const VIEWPORT = { width: 1280, height: 800 }
const DEMO_STEP_DELAY_MS = 250

export default defineConfig({
  // The flows share one database (sync, edit, delete), so they run one at a time.
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    ...devices['Desktop Chrome'],
    baseURL: BASE_URL,
    viewport: VIEWPORT,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      // Fast regression flows: npm test
      name: 'checks',
      testDir: './tests',
      timeout: 30_000,
      use: { video: 'retain-on-failure' },
    },
    {
      // Narrated chapters recorded into docs/demo: npm run demo
      name: 'demo',
      testDir: './demo',
      testMatch: '*.demo.ts',
      timeout: 300_000,
      use: {
        headless: false,
        video: { mode: 'on', size: VIEWPORT },
        launchOptions: { slowMo: DEMO_STEP_DELAY_MS },
      },
    },
  ],
})
