import { NavLink, Outlet } from 'react-router'
import { isAuthenticated, useAuthStore } from '../features/auth/authStore'
import { UserMenu } from '../features/auth/UserMenu'

const navLink = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-sky-50 text-sky-700' : 'text-slate-600 hover:text-slate-900'}`

export function AppLayout() {
  const authenticated = useAuthStore(isAuthenticated)

  return (
    <div className="min-h-dvh bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-semibold">Poke App</h1>
            <nav aria-label="Main" className="flex gap-1">
              <NavLink to="/" end className={navLink}>
                Catalog
              </NavLink>
              {authenticated && (
                <NavLink to="/my-pokedex" className={navLink}>
                  My Pokedex
                </NavLink>
              )}
            </nav>
          </div>
          <UserMenu />
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
