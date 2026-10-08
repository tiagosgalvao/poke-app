import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { isAuthenticated, useAuthStore } from './authStore'

export function RequireAuth({ children }: { children: ReactNode }) {
  const authenticated = useAuthStore(isAuthenticated)
  const location = useLocation()

  if (!authenticated) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return children
}
