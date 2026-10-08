import { cleanup, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../auth/authStore'
import { PokemonDetailPage } from '../catalog/PokemonDetailPage'
import { localPokemon, page, pikachuDetail } from '../../test/fixtures'
import { renderRoutes } from '../../test/render'
import { server } from '../../test/server'
import { EditLocalPokemonPage } from './EditLocalPokemonPage'
import { LocalPokemonPage } from './LocalPokemonPage'

const routes = [
  { path: '/my-pokedex', element: <LocalPokemonPage /> },
  { path: '/my-pokedex/:id/edit', element: <EditLocalPokemonPage /> },
  { path: '/pokemon/:idOrName', element: <PokemonDetailPage /> },
]

const annotatedPikachu = localPokemon(25, 'pikachu', {
  localizedName: 'ピカチュウ',
  region: 'Kanto',
  habitat: 'forest',
  tags: ['mascot', 'starter'],
  notes: "Ash's partner",
  version: 3,
})

describe('local Pokedex', () => {
  beforeEach(() =>
    useAuthStore.getState().login({ token: 'jwt', username: 'ash', expiresAt: new Date(Date.now() + 3_600_000).toISOString() }),
  )
  afterEach(() => {
    cleanup()
    useAuthStore.getState().logout()
  })

  it('lists local Pokemon with their proprietary data', async () => {
    server.use(http.get('*/api/v1/local-pokemon', () => HttpResponse.json(page([annotatedPikachu, localPokemon(26, 'raichu')]))))
    renderRoutes(routes, '/my-pokedex')

    const card = await screen.findByRole('article', { name: /pikachu/i })
    expect(within(card).getByText('ピカチュウ')).toBeInTheDocument()
    expect(within(card).getByText('Kanto · forest')).toBeInTheDocument()
    expect(within(card).getByText('mascot')).toBeInTheDocument()
    expect(within(card).getByRole('link', { name: /edit/i })).toHaveAttribute('href', '/my-pokedex/25/edit')
    expect(screen.getByRole('article', { name: /raichu/i })).toBeInTheDocument()
  })

  it('asks for confirmation before deleting', async () => {
    let deleted = false
    server.use(
      http.get('*/api/v1/local-pokemon', () => HttpResponse.json(page(deleted ? [] : [annotatedPikachu]))),
      http.delete('*/api/v1/local-pokemon/25', () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderRoutes(routes, '/my-pokedex')

    const card = await screen.findByRole('article', { name: /pikachu/i })
    await userEvent.click(within(card).getByRole('button', { name: /delete/i }))
    expect(deleted).toBe(false)
    await userEvent.click(within(card).getByRole('button', { name: /confirm delete/i }))

    expect(await screen.findByText(/your pokedex is empty/i)).toBeInTheDocument()
    expect(deleted).toBe(true)
  })

  it('syncs a list of ids and shows the summary', async () => {
    let requested: unknown = null
    server.use(
      http.get('*/api/v1/local-pokemon', () => HttpResponse.json(page([]))),
      http.post('*/api/v1/local-pokemon/sync', async ({ request }) => {
        requested = await request.json()
        return HttpResponse.json({ created: [1, 4], refreshed: [7], failed: [99999] })
      }),
    )
    renderRoutes(routes, '/my-pokedex')

    await userEvent.type(await screen.findByLabelText(/pokemon ids/i), '1, 4, 7, 99999')
    await userEvent.click(screen.getByRole('button', { name: /^sync$/i }))

    expect(await screen.findByRole('status')).toHaveTextContent('2 created, 1 refreshed, 1 failed (99999)')
    expect(requested).toEqual({ ids: [1, 4, 7, 99999] })
  })

  it('validates the sync ids before calling the API', async () => {
    server.use(http.get('*/api/v1/local-pokemon', () => HttpResponse.json(page([]))))
    renderRoutes(routes, '/my-pokedex')

    await userEvent.type(await screen.findByLabelText(/pokemon ids/i), '1, pikachu')
    await userEvent.click(screen.getByRole('button', { name: /^sync$/i }))

    expect(await screen.findByText(/comma-separated numbers/i)).toBeInTheDocument()
  })

  it('edits the proprietary data with the current version', async () => {
    let body: unknown = null
    server.use(
      http.get('*/api/v1/local-pokemon/25', () => HttpResponse.json(annotatedPikachu)),
      http.put('*/api/v1/local-pokemon/25', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ ...annotatedPikachu, region: 'Johto', version: 4 })
      }),
      http.get('*/api/v1/local-pokemon', () => HttpResponse.json(page([annotatedPikachu]))),
    )
    const { router } = renderRoutes(routes, '/my-pokedex/25/edit')

    const region = await screen.findByLabelText(/region/i)
    expect(region).toHaveValue('Kanto')
    expect(screen.getByLabelText(/tags/i)).toHaveValue('mascot, starter')
    await userEvent.clear(region)
    await userEvent.type(region, 'Johto')
    await userEvent.clear(screen.getByLabelText(/habitat/i))
    await userEvent.click(screen.getByRole('button', { name: /save/i }))

    await screen.findByRole('article', { name: /pikachu/i })
    expect(router.state.location.pathname).toBe('/my-pokedex')
    expect(body).toEqual({
      version: 3,
      localizedName: 'ピカチュウ',
      region: 'Johto',
      habitat: null,
      tags: ['mascot', 'starter'],
      notes: "Ash's partner",
    })
  })

  it('shows server validation errors next to the fields', async () => {
    server.use(
      http.get('*/api/v1/local-pokemon/25', () => HttpResponse.json(annotatedPikachu)),
      http.put('*/api/v1/local-pokemon/25', () =>
        HttpResponse.json(
          { status: 400, detail: 'Validation failed', fieldErrors: [{ field: 'notes', message: 'size must be between 0 and 1000' }] },
          { status: 400 },
        ),
      ),
    )
    renderRoutes(routes, '/my-pokedex/25/edit')

    await userEvent.click(await screen.findByRole('button', { name: /save/i }))

    expect(await screen.findByText('size must be between 0 and 1000')).toBeInTheDocument()
  })

  it('explains a conflicting edit and reloads the latest version', async () => {
    let version = 3
    server.use(
      http.get('*/api/v1/local-pokemon/25', () => HttpResponse.json({ ...annotatedPikachu, version, region: version > 3 ? 'Johto' : 'Kanto' })),
      http.put('*/api/v1/local-pokemon/25', () => {
        version = 4
        return HttpResponse.json(
          { status: 409, detail: 'Local Pokemon 25 was modified meanwhile (version 4, not 3). Reload it and try again.' },
          { status: 409 },
        )
      }),
    )
    renderRoutes(routes, '/my-pokedex/25/edit')

    await userEvent.click(await screen.findByRole('button', { name: /save/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('modified meanwhile')
    await userEvent.click(screen.getByRole('button', { name: /reload latest/i }))

    expect(await screen.findByDisplayValue('Johto')).toBeInTheDocument()
  })

  it('adds a Pokemon to the local Pokedex from its detail page', async () => {
    let imported: unknown = null
    server.use(
      http.get('*/api/v1/pokemon/pikachu', () => HttpResponse.json(pikachuDetail)),
      http.post('*/api/v1/local-pokemon', async ({ request }) => {
        imported = await request.json()
        return HttpResponse.json(localPokemon(25, 'pikachu'), { status: 201 })
      }),
    )
    renderRoutes(routes, '/pokemon/pikachu')

    await userEvent.click(await screen.findByRole('button', { name: /add to my pokedex/i }))

    expect(await screen.findByRole('status')).toHaveTextContent(/added to your pokedex/i)
    expect(imported).toEqual({ idOrName: 'pikachu' })
  })

  it('tells the user when the Pokemon is already in the local Pokedex', async () => {
    server.use(
      http.get('*/api/v1/pokemon/pikachu', () => HttpResponse.json(pikachuDetail)),
      http.post('*/api/v1/local-pokemon', () =>
        HttpResponse.json({ status: 409, detail: 'Pokemon 25 is already in the local Pokedex' }, { status: 409 }),
      ),
    )
    renderRoutes(routes, '/pokemon/pikachu')

    await userEvent.click(await screen.findByRole('button', { name: /add to my pokedex/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('already in the local Pokedex')
  })
})
