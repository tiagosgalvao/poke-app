import { test as base, expect, type APIRequestContext, type Page } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { basename, dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

export const ASH = { username: 'ash', password: 'Pikachu123!' }

const AUTH_STORAGE_KEY = 'poke-app-auth'
const DEMO_PROJECT = 'demo'
const CHAPTER_SUFFIX = '.demo.ts'
const MAX_LIST_PAGES = 10
// Keeps the final screen of each recording on view instead of cutting right after the last check.
const DEMO_FINAL_SCREEN_MS = 2_000
const FFMPEG = process.env.FFMPEG ?? 'ffmpeg'
// H.264 in MP4 plays everywhere (QuickTime, browsers, GitHub); faststart lets it play while downloading.
const MP4_OPTIONS = ['-c:v', 'libx264', '-preset', 'slow', '-crf', '23', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', '-an']
const DEMO_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'docs', 'demo')
// Chrome logs every failed HTTP response itself; the flows trigger some on purpose (a 409 conflict, a 401 login).
const EXPECTED_NETWORK_ERROR = /^Failed to load resource: the server responded with a status of (401|409)/

export interface Session {
  token: string
  username: string
  expiresAt: string
}

export interface LocalPokemon {
  id: number
  name: string
  version: number
  localizedName: string | null
  region: string | null
  habitat: string | null
  tags: string[]
  notes: string | null
}

function bearer(session: Session) {
  return { Authorization: `Bearer ${session.token}` }
}

export class PokeApi {
  constructor(private readonly request: APIRequestContext) {}

  async login({ username, password }: typeof ASH): Promise<Session> {
    const response = await this.request.post('/api/v1/auth/login', { data: { username, password } })
    expect(response.ok(), `login as ${username}`).toBeTruthy()
    const body = await response.json()
    return { token: body.accessToken, username, expiresAt: body.expiresAt }
  }

  async localPokemon(id: number): Promise<LocalPokemon | null> {
    const response = await this.request.get(`/api/v1/local-pokemon/${id}`)
    return response.ok() ? response.json() : null
  }

  // Puts a record's proprietary data back to how a test found it, whatever version it reached meanwhile.
  async restoreProprietaryData(original: LocalPokemon, session: Session) {
    const current = await this.localPokemon(original.id)
    expect(current, `local Pokemon ${original.id}`).not.toBeNull()
    const { localizedName, region, habitat, tags, notes } = original
    const response = await this.request.put(`/api/v1/local-pokemon/${original.id}`, {
      headers: bearer(session),
      data: { version: current!.version, localizedName, region, habitat, tags, notes },
    })
    expect(response.ok(), `restore local Pokemon ${original.id}`).toBeTruthy()
  }

  async patchLocalPokemon(id: number, changes: Partial<LocalPokemon> & { version: number }, session: Session) {
    const response = await this.request.patch(`/api/v1/local-pokemon/${id}`, { headers: bearer(session), data: changes })
    expect(response.ok(), `patch local Pokemon ${id}`).toBeTruthy()
  }

  async deleteLocalPokemon(id: number, session: Session) {
    await this.request.delete(`/api/v1/local-pokemon/${id}`, { headers: bearer(session) })
  }
}

// Starts the page already logged in, without going through the login form.
export async function signIn(page: Page, session: Session) {
  await page.addInitScript(
    ([key, value]) => window.localStorage.setItem(key, value),
    [AUTH_STORAGE_KEY, JSON.stringify({ state: session, version: 0 })] as const,
  )
}

// Playwright records WebM; the demo videos are published as MP4.
function toMp4(webm: string, mp4: string) {
  try {
    execFileSync(FFMPEG, ['-loglevel', 'error', '-y', '-i', webm, ...MP4_OPTIONS, mp4])
  } catch (error) {
    throw new Error(`Converting the demo video needs ffmpeg (brew install ffmpeg, or set FFMPEG to its path): ${error}`)
  }
}

// The local list is paginated, so a record can sit on a later page.
export async function findCard(page: Page, name: string) {
  const card = page.getByRole('article', { name })
  for (let pages = 1; pages < MAX_LIST_PAGES && !(await card.isVisible()); pages++) {
    const next = page.getByRole('button', { name: 'Next' })
    if (!(await next.isVisible()) || (await next.isDisabled())) {
      break
    }
    await next.click()
    await page.waitForLoadState('networkidle')
  }
  await expect(card).toBeVisible()
  return card
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

  // Keeps each demo chapter's video as docs/demo/<chapter file name>.mp4.
  demoRecording: [
    async ({ page }, use, testInfo) => {
      await use()
      const video = page.video()
      if (testInfo.project.name !== DEMO_PROJECT || !video) {
        return
      }
      await page.waitForTimeout(DEMO_FINAL_SCREEN_MS)
      await page.close()
      const recording = testInfo.outputPath('recording.webm')
      await video.saveAs(recording)
      mkdirSync(DEMO_DIR, { recursive: true })
      toMp4(recording, join(DEMO_DIR, `${basename(testInfo.file, CHAPTER_SUFFIX)}.mp4`))
    },
    { auto: true },
  ],
})

export { expect }
