import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { pikachuDetail } from '../../test/fixtures'
import { renderRoutes } from '../../test/render'
import { server } from '../../test/server'
import { PokemonDetailPage } from './PokemonDetailPage'

const routes = [{ path: '/pokemon/:idOrName', element: <PokemonDetailPage /> }]

describe('PokemonDetailPage', () => {
  it('shows the artwork, stats, description and evolution chain', async () => {
    server.use(http.get('*/api/v1/pokemon/pikachu', () => HttpResponse.json(pikachuDetail)))
    renderRoutes(routes, '/pokemon/pikachu')

    expect(await screen.findByRole('heading', { level: 2, name: 'Pikachu' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Pikachu artwork' })).toHaveAttribute('src', 'https://img/art/25.png')
    expect(screen.getByText('Possesses cheek sacs in which it stores electricity.')).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'speed' })).toHaveAttribute('aria-valuenow', '90')
    expect(screen.getByRole('progressbar', { name: 'hp' })).toHaveAttribute('aria-valuenow', '35')

    const evolution = screen.getByRole('list', { name: 'Evolution chain' })
    expect(within(evolution).getByRole('link', { name: /pichu/i })).toHaveAttribute('href', '/pokemon/pichu')
    expect(within(evolution).getByRole('link', { name: /raichu/i })).toHaveAttribute('href', '/pokemon/raichu')
    expect(within(evolution).getByText('use-item: thunder-stone')).toBeInTheDocument()
    expect(within(evolution).getByRole('link', { name: /^pikachu/i })).toHaveAttribute('aria-current', 'page')
  })

  it('navigates along the evolution chain', async () => {
    server.use(
      http.get('*/api/v1/pokemon/pikachu', () => HttpResponse.json(pikachuDetail)),
      http.get('*/api/v1/pokemon/raichu', () =>
        HttpResponse.json({ ...pikachuDetail, id: 26, name: 'raichu', description: 'Raichu stores electricity.' }),
      ),
    )
    renderRoutes(routes, '/pokemon/pikachu')

    await userEvent.click(await screen.findByRole('link', { name: /raichu/i }))

    expect(await screen.findByText('Raichu stores electricity.')).toBeInTheDocument()
  })

  it('tells the user when the Pokemon does not exist', async () => {
    server.use(
      http.get('*/api/v1/pokemon/missingno', () =>
        HttpResponse.json({ status: 404, detail: "Pokemon 'missingno' was not found" }, { status: 404 }),
      ),
    )
    renderRoutes(routes, '/pokemon/missingno')

    expect(await screen.findByRole('heading', { name: /pokemon not found/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /back to the catalog/i })).toHaveAttribute('href', '/')
  })
})
