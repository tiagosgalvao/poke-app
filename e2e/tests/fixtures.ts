import { test as base, expect, type APIRequestContext, type Page } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

export const ASH = { username: 'ash', password: 'Pikachu123!' }

const AUTH_STORAGE_KEY = 'poke-app-auth'
const DEMO_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'docs', 'demo')
// Chrome logs every failed HTTP response itself; the flows trigger some on purpose (a 409 conflict, a 401 login).
const EXPECTED_NETWORK_ERROR = /^Failed to load resource: the server responded with a status of (401|409)/

interface Session {
  token: string
  username: string
  expiresAt: string
}

export class PokeApi {
  constructor(private readonly request: APIRequestContext) {}

  async login({ username, password }: typeof ASH): Promise<Session> {
    const response = await this.request.post('/api/v1/auth/login', { data: { username, password } })
    expect(response.ok(), `login as ${username}`).toBeTruthy()
    const body = await response.json()
    return { token: body.accessToken, username, expiresAt: body.expiresAt }
  }

  async localPokemon(id: number) {
    const response = await this.request.get(`/api/v1/local-pokemon/${id}`)
    return response.ok() ? response.json() : null
  }

  async deleteLocalPokemon(id: number, session: Session) {
    await this.request.delete(`/api/v1/local-pokemon/${id}`, { headers: { Authorization: `Bearer ${session.token}` } })
  }
}

// Starts the page already logged in, without going through the login form.
export async function signIn(page: Page, session: Session) {
  await page.addInitScript(
    ([key, value]) => window.localStorage.setItem(key, value),
    [AUTH_STORAGE_KEY, JSON.stringify({ state: session, version: 0 })] as const,
  )
}

function slug(title: string) {
  return title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
}

export const test = base.extend<{ api: PokeApi; consoleGuard: void; demoRecording: void }>({
  api: async ({ request }, use) => {
    await use(new PokeApi(request))
  },

  // Every flow fails if the browser logs an error or warning, like the unit tests do.
  consoleGuard: [
    async ({ page }, use) => {
      const problems: string[] = []
      page.on('console', (message) => {
        const type = message.type()
        if ((type === 'error' || type === 'warning') && !EXPECTED_NETWORK_ERROR.test(message.text())) {
          problems.push(`${type}: ${message.text()}`)
        }
      })
      page.on('pageerror', (error) => problems.push(`pageerror: ${error.message}`))
      await use()
      expect(problems, 'browser console errors and warnings').toEqual([])
    },
    { auto: true },
  ],

  // In demo mode, keeps each flow's video under docs/demo/ named after the test.
  demoRecording: [
    async ({ page }, use, testInfo) => {
      await use()
      const video = page.video()
      if (process.env.DEMO !== '1' || !video) {
        return
      }
      await page.close()
      mkdirSync(DEMO_DIR, { recursive: true })
      await video.saveAs(join(DEMO_DIR, `${slug(testInfo.titlePath.slice(1).join(' '))}.webm`))
    },
    { auto: true },
  ],
})

export { expect }
