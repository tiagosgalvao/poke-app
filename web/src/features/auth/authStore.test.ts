import { afterEach, describe, expect, it } from 'vitest'
import { isAuthenticated, useAuthStore } from './authStore'

const IN_ONE_HOUR = new Date(Date.now() + 3_600_000).toISOString()
const AN_HOUR_AGO = new Date(Date.now() - 3_600_000).toISOString()

describe('authStore', () => {
  afterEach(() => useAuthStore.getState().logout())

  it('keeps the session after login and clears it on logout', () => {
    useAuthStore.getState().login({ token: 'jwt', username: 'ash', expiresAt: IN_ONE_HOUR })

    expect(isAuthenticated(useAuthStore.getState())).toBe(true)
    expect(useAuthStore.getState().username).toBe('ash')

    useAuthStore.getState().logout()

    expect(isAuthenticated(useAuthStore.getState())).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
  })

  it('treats an expired token as logged out', () => {
    useAuthStore.getState().login({ token: 'jwt', username: 'ash', expiresAt: AN_HOUR_AGO })

    expect(isAuthenticated(useAuthStore.getState())).toBe(false)
  })
})
