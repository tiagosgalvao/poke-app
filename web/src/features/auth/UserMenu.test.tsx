import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { renderRoutes } from '../../test/render'
import { useAuthStore } from './authStore'
import { RequireAuth } from './RequireAuth'
import { UserMenu } from './UserMenu'

const routes = [
  { path: '/', element: <UserMenu /> },
  { path: '/login', element: <p>login page</p> },
  {
    path: '/private',
    element: (
      <RequireAuth>
        <p>private</p>
      </RequireAuth>
    ),
  },
]

describe('UserMenu', () => {
  afterEach(() => {
    cleanup()
    useAuthStore.getState().logout()
  })

  it('offers login and sign up to visitors', () => {
    renderRoutes(routes, '/')

    expect(screen.getByRole('link', { name: /log in/i })).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: /sign up/i })).toHaveAttribute('href', '/register')
  })

  it('shows the user and logs out', async () => {
    useAuthStore.getState().login({ token: 'jwt', username: 'ash', expiresAt: new Date(Date.now() + 60_000).toISOString() })
    renderRoutes(routes, '/')

    expect(screen.getByText('ash')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /log out/i }))

    expect(useAuthStore.getState().token).toBeNull()
    expect(screen.getByRole('link', { name: /log in/i })).toBeInTheDocument()
  })

  it('sends a user with an expired session back to the login page', () => {
    useAuthStore.getState().login({ token: 'jwt', username: 'ash', expiresAt: new Date(Date.now() - 60_000).toISOString() })
    const { router } = renderRoutes(routes, '/private')

    expect(screen.getByText('login page')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?redirect=%2Fprivate')
  })
})
