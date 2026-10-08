import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll, beforeEach, vi } from 'vitest'
import { server } from './server'

const consoleProblems: string[] = []

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

beforeEach(() => {
  consoleProblems.length = 0
  vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => consoleProblems.push(`error: ${args.join(' ')}`))
  vi.spyOn(console, 'warn').mockImplementation((...args: unknown[]) => consoleProblems.push(`warn: ${args.join(' ')}`))
})

afterEach(() => {
  server.resetHandlers()
  localStorage.clear()
  vi.restoreAllMocks()
  if (consoleProblems.length > 0) {
    throw new Error(`The test logged to the console:\n${consoleProblems.join('\n')}`)
  }
})

afterAll(() => server.close())
