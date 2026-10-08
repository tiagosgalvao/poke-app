import { Link, useNavigate } from 'react-router'
import { secondaryButton } from '../../components/buttonStyles'
import { isAuthenticated, useAuthStore } from './authStore'

export function UserMenu() {
  const authenticated = useAuthStore(isAuthenticated)
  const username = useAuthStore((state) => state.username)
  const logout = useAuthStore((state) => state.logout)
  const navigate = useNavigate()

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
        onClick={() => {
          logout()
          navigate('/')
        }}
      >
        Log out
      </button>
    </div>
  )
}
