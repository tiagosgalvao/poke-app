import { cleanup, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../features/auth/authStore'
import { renderRoutes } from '../test/render'
import { AppLayout } from './AppLayout'

const routes = [{ path: '/', element: <AppLayout />, children: [{ index: true, element: <p>home</p> }] }]

function mainNav() {
  return within(screen.getByRole('navigation', { name: 'Main' }))
}

describe('AppLayout', () => {
  afterEach(() => {
    cleanup()
    useAuthStore.getState().logout()
  })

  it('shows visitors only the public catalog link', () => {
    renderRoutes(routes, '/')

    expect(mainNav().getByRole('link', { name: 'Catalog' })).toHaveAttribute('href', '/')
    expect(mainNav().queryByRole('link', { name: 'My Pokedex' })).not.toBeInTheDocument()
  })

  it('shows My Pokedex to a signed-in user and hides it again after logout', async () => {
    useAuthStore.getState().login({ token: 'jwt', username: 'ash', expiresAt: new Date(Date.now() + 60_000).toISOString() })
    renderRoutes(routes, '/')

    expect(mainNav().getByRole('link', { name: 'My Pokedex' })).toHaveAttribute('href', '/my-pokedex')

    await userEvent.click(screen.getByRole('button', { name: /log out/i }))

    expect(mainNav().queryByRole('link', { name: 'My Pokedex' })).not.toBeInTheDocument()
  })
})
