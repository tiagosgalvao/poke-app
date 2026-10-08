import { cleanup, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { renderRoutes } from '../../test/render'
import { useAuthStore } from './authStore'
import { LoginPage } from './LoginPage'
import { RegisterPage } from './RegisterPage'
import { RequireAuth } from './RequireAuth'

const routes = [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/my-pokedex',
    element: (
      <RequireAuth>
        <p>secret pokedex</p>
      </RequireAuth>
    ),
  },
]

const token = { accessToken: 'jwt', tokenType: 'Bearer', expiresAt: new Date(Date.now() + 3_600_000).toISOString() }

describe('authentication pages', () => {
  afterEach(() => {
    cleanup()
    useAuthStore.getState().logout()
  })

  it('redirects a visitor to the login page and back after logging in', async () => {
    server.use(http.post('*/api/v1/auth/login', () => HttpResponse.json(token)))
    const { router } = renderRoutes(routes, '/my-pokedex')

    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText(/username/i), 'ash')
    await userEvent.type(screen.getByLabelText(/password/i), 'Pikachu123!')
    await userEvent.click(screen.getByRole('button', { name: /log in/i }))

    expect(await screen.findByText('secret pokedex')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/my-pokedex')
    expect(useAuthStore.getState().username).toBe('ash')
  })

  it('shows the server error for bad credentials', async () => {
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json({ status: 401, detail: 'Invalid username or password' }, { status: 401 }),
      ),
    )
    renderRoutes(routes, '/login')

    await userEvent.type(await screen.findByLabelText(/username/i), 'ash')
    await userEvent.type(screen.getByLabelText(/password/i), 'WrongPass1!')
    await userEvent.click(screen.getByRole('button', { name: /log in/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password')
    expect(useAuthStore.getState().token).toBeNull()
  })

  it('validates the registration form before calling the API', async () => {
    renderRoutes(routes, '/register')

    await userEvent.type(await screen.findByLabelText(/username/i), 'a b')
    await userEvent.type(screen.getByLabelText(/email/i), 'not-an-email')
    await userEvent.type(screen.getByLabelText(/password/i), 'short')
    await userEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/3 to 30 letters/i)).toBeInTheDocument()
    expect(screen.getByText(/valid email/i)).toBeInTheDocument()
    expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument()
  })

  it('registers, logs in and opens the pokedex', async () => {
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json({ id: 'u-1', username: 'misty', email: 'misty@cerulean.city', createdAt: '' }, { status: 201 }),
      ),
      http.post('*/api/v1/auth/login', () => HttpResponse.json(token)),
    )
    const { router } = renderRoutes(routes, '/register')

    await userEvent.type(await screen.findByLabelText(/username/i), 'misty')
    await userEvent.type(screen.getByLabelText(/email/i), 'misty@cerulean.city')
    await userEvent.type(screen.getByLabelText(/password/i), 'Starmie123!')
    await userEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText('secret pokedex')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/my-pokedex')
  })

  it('shows a taken username next to the field', async () => {
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json({ status: 409, detail: "Username 'misty' is already taken" }, { status: 409 }),
      ),
    )
    renderRoutes(routes, '/register')

    await userEvent.type(await screen.findByLabelText(/username/i), 'misty')
    await userEvent.type(screen.getByLabelText(/email/i), 'misty@cerulean.city')
    await userEvent.type(screen.getByLabelText(/password/i), 'Starmie123!')
    await userEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent("Username 'misty' is already taken")
  })
})
