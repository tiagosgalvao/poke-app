import { defineConfig, devices } from '@playwright/test'

const DEMO = process.env.DEMO === '1'
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000'
const DEMO_STEP_DELAY_MS = 600
const VIEWPORT = { width: 1280, height: 800 }

export default defineConfig({
  testDir: './tests',
  // The flows share one database (sync, edit, delete), so they run one at a time.
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  timeout: DEMO ? 120_000 : 30_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: BASE_URL,
    viewport: VIEWPORT,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: DEMO ? { mode: 'on', size: VIEWPORT } : 'retain-on-failure',
    headless: !DEMO,
    launchOptions: { slowMo: DEMO ? DEMO_STEP_DELAY_MS : 0 },
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: VIEWPORT } }],
})
