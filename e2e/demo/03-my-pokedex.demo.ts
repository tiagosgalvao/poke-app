import { ASH, expect, findCard, test, type LocalPokemon, type Session } from '../tests/fixtures'
import { Narrator } from './narrator'

const BULBASAUR = 1
const DITTO = 132

let session: Session
let bulbasaur: LocalPokemon

test.beforeEach(async ({ api }) => {
  session = await api.login(ASH)
  bulbasaur = (await api.localPokemon(BULBASAUR))!
  await api.deleteLocalPokemon(DITTO, session)
})

test.afterEach(async ({ api }) => {
  await api.restoreProprietaryData(bulbasaur, session)
  await api.deleteLocalPokemon(DITTO, session)
})

test('chapter 3: my pokedex', async ({ api, page }) => {
  const narrator = await Narrator.attach(page)
  await page.goto('/login')
  await page.getByLabel('Username').fill(ASH.username)
  await page.getByLabel('Password').fill(ASH.password)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL(/\/my-pokedex$/)

  await narrator.say('Chapter 3 · My Pokedex. Our own copy of Pokemon, with data PokeAPI does not have (US03, US04).')
  await narrator.say(
    'Stored in our Postgres database, with proprietary fields: localized name, region, habitat, tags and notes.',
    await findCard(page, 'Bulbasaur'),
  )

  await narrator.type(page.getByLabel('Pokemon ids'), '1, 4, 7', 'Sync pulls Pokemon from PokeAPI: missing ones are created, existing ones refreshed.')
  await narrator.click(page.getByRole('button', { name: 'Sync' }))
  await expect(page.getByRole('status')).toHaveText(/^\d created, \d refreshed, 0 failed$/)
  await narrator.say('A sync never overwrites the proprietary data.', page.getByRole('status'))

  await narrator.say('A single Pokemon can also be added from its detail page.')
  await page.goto('/pokemon/ditto')
  await narrator.click(page.getByRole('button', { name: 'Add to My Pokedex' }), 'Add Ditto to My Pokedex.')
  await expect(page.getByRole('status')).toContainText('Added to your Pokedex.')
  await narrator.say('Imported from PokeAPI into the local database.', page.getByRole('status'))

  await page.getByRole('link', { name: 'Open My Pokedex' }).click()
  await narrator.click((await findCard(page, 'Bulbasaur')).getByRole('link', { name: 'Edit' }), "Let's edit Bulbasaur's proprietary data.")
  await expect(page.getByRole('heading', { name: 'Edit Bulbasaur' })).toBeVisible()

  const tags = page.getByLabel('Tags (comma separated)')
  await narrator.type(tags, 'starter, not a tag!', 'Tags may only use letters, digits and hyphens. This one breaks the rule.')
  await narrator.click(page.getByRole('button', { name: 'Save' }))
  const tagError = page.getByText('Tags use letters, digits and hyphens (up to 30 each)')
  await expect(tagError).toBeVisible()
  await narrator.say('The form catches it. The API enforces the same rules and would answer 400 with field errors.', tagError)

  await narrator.type(tags, 'starter, grass', 'Fixed.')
  await narrator.type(page.getByLabel('Notes'), 'Loves sunny days')
  await narrator.click(page.getByRole('button', { name: 'Save' }))
  await expect(page).toHaveURL(/\/my-pokedex$/)
  const card = await findCard(page, 'Bulbasaur')
  await expect(card).toContainText('Loves sunny days')
  await narrator.say('Saved. Every update carries the version it was based on.', card)

  await narrator.click(card.getByRole('link', { name: 'Edit' }), 'What if two people edit at the same time?')
  await expect(page.getByRole('heading', { name: 'Edit Bulbasaur' })).toBeVisible()
  await narrator.say('This form was loaded at the current version.', page.getByText(/\(version \d+\)/))
  await narrator.say('Meanwhile, someone else saves Bulbasaur in another tab…')
  const current = (await api.localPokemon(BULBASAUR))!
  await api.patchLocalPokemon(BULBASAUR, { version: current.version, notes: 'Changed in another tab' }, session)

  await narrator.type(page.getByLabel('Region'), 'Johto', 'We change the region and save, unaware of that.')
  await narrator.click(page.getByRole('button', { name: 'Save' }))
  await expect(page.getByRole('alert')).toBeVisible()
  await narrator.say('The API refuses the stale version with 409 Conflict instead of silently overwriting.', page.getByRole('alert'))
  await narrator.click(page.getByRole('button', { name: 'Reload latest' }), 'Reload the latest version and decide again.')
  await expect(page.getByLabel('Notes')).toHaveValue('Changed in another tab')
  await narrator.say("The other tab's change is now visible.", page.getByLabel('Notes'))

  await narrator.click(page.getByRole('link', { name: 'Cancel' }), 'Back to the list.')
  const ditto = await findCard(page, 'Ditto')
  await narrator.click(ditto.getByRole('button', { name: 'Delete' }), 'Deleting asks for confirmation in place, with no pop-up.')
  await narrator.click(ditto.getByRole('button', { name: 'Confirm delete' }))
  await expect(page.getByRole('article', { name: 'Ditto' })).toHaveCount(0)
  await narrator.say("That's the tour: a cached catalog, accounts with protected routes, and a local Pokedex with safe edits.")
})
