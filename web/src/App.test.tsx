import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import App from './App'
import { page, summary } from './test/fixtures'
import { server } from './test/server'

describe('App', () => {
  it('renders the application shell with the catalog', async () => {
    server.use(http.get('*/api/v1/pokemon', () => HttpResponse.json(page([summary(25, 'pikachu')]))))

    render(<App />)

    expect(screen.getByRole('heading', { name: /poke app/i })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: /pikachu/i })).toBeInTheDocument()
  })
})
