import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import { configureApiClient } from '../../api/client'

interface Session {
  token: string
  username: string
  expiresAt: string
}

interface AuthState {
  token: string | null
  username: string | null
  expiresAt: string | null
  login: (session: Session) => void
  logout: () => void
}

const STORAGE_KEY = 'poke-app-auth'

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      username: null,
      expiresAt: null,
      login: ({ token, username, expiresAt }) => set({ token, username, expiresAt }),
      logout: () => set({ token: null, username: null, expiresAt: null }),
    }),
    { name: STORAGE_KEY, storage: createJSONStorage(() => localStorage) },
  ),
)

export function isAuthenticated(state: Pick<AuthState, 'token' | 'expiresAt'>) {
  return state.token !== null && state.expiresAt !== null && Date.parse(state.expiresAt) > Date.now()
}

export function connectApiClientToAuth() {
  configureApiClient({
    getToken: () => (isAuthenticated(useAuthStore.getState()) ? useAuthStore.getState().token : null),
    onUnauthorized: () => useAuthStore.getState().logout(),
  })
}
