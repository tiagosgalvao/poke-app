import { useMutation } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router'
import { login } from './authApi'
import { useAuthStore } from './authStore'
import type { LoginForm } from './schemas'

const DEFAULT_DESTINATION = '/my-pokedex'

export function useSessionLogin() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const startSession = useAuthStore((state) => state.login)

  return useMutation({
    mutationFn: (credentials: LoginForm) => login(credentials),
    onSuccess: (token, credentials) => {
      startSession({ token: token.accessToken, username: credentials.username.trim().toLowerCase(), expiresAt: token.expiresAt })
      navigate(searchParams.get('redirect') ?? DEFAULT_DESTINATION, { replace: true })
    },
  })
}
