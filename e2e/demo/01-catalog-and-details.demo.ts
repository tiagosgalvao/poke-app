import { expect, test } from '../tests/fixtures'
import { Narrator } from './narrator'

test('chapter 1: catalog and details', async ({ page }) => {
  const narrator = await Narrator.attach(page)
  await page.goto('/')
  await expect(page.getByRole('link', { name: /bulbasaur/i })).toBeVisible()

  await narrator.say('Chapter 1 · Catalog and details. Browsing Pokemon from PokeAPI, no account needed.')
  await narrator.say(
    'The catalog lists every Pokemon from PokeAPI, 20 per page (US01). Responses are cached in Redis, so repeat visits are fast.',
    page.getByText(/Pokemon from PokeAPI$/),
  )

  const bulbasaur = page.getByRole('link', { name: /bulbasaur/i })
  await narrator.say('Each card shows the sprite, the category, the weight and the abilities.', bulbasaur)
  await narrator.say('A star marks a hidden ability.', bulbasaur.getByRole('list', { name: 'Abilities' }))

  await narrator.click(page.getByRole('button', { name: 'Next' }), 'Pagination is kept in the URL, so any page can be bookmarked or shared.')
  await expect(page).toHaveURL(/\?page=2$/)
  await narrator.say('Page 2, straight from the address (?page=2).', page.getByText(/^Page 2 of \d+$/))

  await narrator.say('Now the detail view of a Pokemon (US02).')
  await page.goto('/pokemon/eevee')
  await expect(page.getByRole('heading', { name: 'Eevee', level: 2 })).toBeVisible()

  await narrator.say('Official artwork and types…', page.getByRole('img', { name: 'Eevee artwork' }))
  await narrator.say('…number, category and the Pokedex description.', page.getByText(/Harbors the potential/))
  await narrator.say(
    'Base stats are drawn as accessible progress bars.',
    page.getByRole('heading', { name: 'Base stats' }).locator('..'),
  )
  const chain = page.getByRole('list', { name: 'Evolution chain' })
  await narrator.say('Eevee has a branching evolution chain: every evolution, its trigger, and the current one highlighted.', chain)

  await narrator.click(chain.getByRole('link', { name: /vaporeon/i }), 'Each evolution opens its own page.')
  await expect(page.getByRole('heading', { name: 'Vaporeon', level: 2 })).toBeVisible()
  await narrator.say(
    'Visitors can browse everything. Saving a Pokemon to My Pokedex needs an account.',
    page.getByRole('link', { name: 'Log in to add it to My Pokedex' }),
  )
  await narrator.say('Next: accounts and protected routes.')
})
