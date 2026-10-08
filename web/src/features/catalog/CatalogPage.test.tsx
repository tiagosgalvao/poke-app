import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { page, summary } from '../../test/fixtures'
import { renderRoutes } from '../../test/render'
import { server } from '../../test/server'
import { CatalogPage } from './CatalogPage'

const routes = [{ path: '/', element: <CatalogPage /> }]

describe('CatalogPage', () => {
  it('shows each Pokemon with sprite, category, weight and abilities', async () => {
    server.use(
      http.get('*/api/v1/pokemon', () =>
        HttpResponse.json(page([summary(25, 'pikachu'), summary(26, 'raichu', { weightKg: 30 })], 0, 1351)),
      ),
    )
    renderRoutes(routes, '/')

    const pikachu = await screen.findByRole('link', { name: /pikachu/i })
    expect(within(pikachu).getByRole('img', { name: 'pikachu' })).toHaveAttribute('src', 'https://img/25.png')
    expect(within(pikachu).getByText('#025')).toBeInTheDocument()
    expect(within(pikachu).getByText('Mouse Pokémon')).toBeInTheDocument()
    expect(within(pikachu).getByText('6 kg')).toBeInTheDocument()
    expect(within(pikachu).getByText('static')).toBeInTheDocument()
    expect(within(pikachu).getByText(/lightning-rod/)).toHaveAttribute('title', 'Hidden ability')
    expect(pikachu).toHaveAttribute('href', '/pokemon/pikachu')
    expect(screen.getByText('Page 1 of 68')).toBeInTheDocument()
  })

  it('moves to the next page through the URL', async () => {
    server.use(
      http.get('*/api/v1/pokemon', ({ request }) => {
        const requested = Number(new URL(request.url).searchParams.get('page'))
        const first = requested === 0 ? summary(1, 'bulbasaur') : summary(21, 'spearow')
        return HttpResponse.json(page([first], requested, 1351))
      }),
    )
    const { router } = renderRoutes(routes, '/')

    await screen.findByRole('link', { name: /bulbasaur/i })
    await userEvent.click(screen.getByRole('button', { name: /next/i }))

    expect(await screen.findByRole('link', { name: /spearow/i })).toBeInTheDocument()
    expect(router.state.location.search).toBe('?page=2')
    expect(screen.getByRole('button', { name: /previous/i })).toBeEnabled()
  })

  it('opens the requested page from the URL', async () => {
    server.use(
      http.get('*/api/v1/pokemon', ({ request }) => {
        const requested = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json(page([summary(41, `page-${requested}`)], requested, 1351))
      }),
    )
    renderRoutes(routes, '/?page=3')

    expect(await screen.findByRole('link', { name: /page-2/i })).toBeInTheDocument()
  })

  it('shows an error with a retry when the catalog is unavailable', async () => {
    let calls = 0
    server.use(
      http.get('*/api/v1/pokemon', () => {
        calls += 1
        return calls === 1
          ? HttpResponse.json({ status: 503, detail: 'The Pokemon catalog is temporarily unavailable.' }, { status: 503 })
          : HttpResponse.json(page([summary(25, 'pikachu')]))
      }),
    )
    renderRoutes(routes, '/')

    expect(await screen.findByRole('alert')).toHaveTextContent('temporarily unavailable')
    await userEvent.click(screen.getByRole('button', { name: /try again/i }))

    expect(await screen.findByRole('link', { name: /pikachu/i })).toBeInTheDocument()
  })
})
