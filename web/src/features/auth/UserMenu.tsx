import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { secondaryButton } from '../../components/buttonStyles'
import { isAuthenticated, useAuthStore } from './authStore'

interface SignOutState {
  signOut?: boolean
}

// Logging out goes home first and clears the session only once home has rendered.
// Clearing it while a protected page is still on screen would make RequireAuth redirect to login.
export function UserMenu() {
  const authenticated = useAuthStore(isAuthenticated)
  const username = useAuthStore((state) => state.username)
  const logout = useAuthStore((state) => state.logout)
  const navigate = useNavigate()
  const location = useLocation()
  const signingOut = (location.state as SignOutState | null)?.signOut === true

  useEffect(() => {
    if (signingOut) {
      logout()
      navigate(location.pathname, { replace: true })
    }
  }, [signingOut, logout, navigate, location.pathname])

  if (!authenticated) {
    return (
      <div className="flex items-center gap-3 text-sm">
        <Link className="font-medium text-sky-700 hover:underline" to="/login">
          Log in
        </Link>
        <Link className={secondaryButton} to="/register">
          Sign up
        </Link>
      </div>
    )
  }
  return (
    <div className="flex items-center gap-3 text-sm">
      <span className="text-slate-600">
        Signed in as <strong className="text-slate-900">{username}</strong>
      </span>
      <button
        type="button"
        className={secondaryButton}
        onClick={() => navigate('/', { state: { signOut: true } satisfies SignOutState })}
      >
        Log out
      </button>
    </div>
  )
}
