import { ASH, expect, findCard, signIn, test, type LocalPokemon, type Session } from './fixtures'

const BULBASAUR = 1
const DITTO = 132

let session: Session

test.describe('my pokedex', () => {
  test.beforeEach(async ({ api, page }) => {
    session = await api.login(ASH)
    await signIn(page, session)
  })

  test('sync Pokemon from PokeAPI', async ({ page }) => {
    await page.goto('/my-pokedex')

    await page.getByLabel('Pokemon ids').fill('1, 4, 7')
    await page.getByRole('button', { name: 'Sync' }).click()

    await expect(page.getByRole('status')).toHaveText(/^\d created, \d refreshed, 0 failed$/)
  })

  test.describe('records that the flow changes', () => {
    let bulbasaur: LocalPokemon

    test.beforeEach(async ({ api }) => {
      bulbasaur = (await api.localPokemon(BULBASAUR))!
      await api.deleteLocalPokemon(DITTO, session)
    })

    test.afterEach(async ({ api }) => {
      await api.restoreProprietaryData(bulbasaur, session)
      await api.deleteLocalPokemon(DITTO, session)
    })

    test('add a Pokemon from its detail page, then delete it', async ({ api, page }) => {
      await page.goto('/pokemon/ditto')

      await page.getByRole('button', { name: 'Add to My Pokedex' }).click()
      await expect(page.getByRole('status')).toContainText('Added to your Pokedex.')
      await page.getByRole('link', { name: 'Open My Pokedex' }).click()

      const ditto = await findCard(page, 'Ditto')
      await ditto.getByRole('button', { name: 'Delete' }).click()
      await ditto.getByRole('button', { name: 'Confirm delete' }).click()

      await expect(page.getByRole('article', { name: 'Ditto' })).toHaveCount(0)
      expect(await api.localPokemon(DITTO)).toBeNull()
    })

    test('edit proprietary data with validation', async ({ page }) => {
      await page.goto(`/my-pokedex/${BULBASAUR}/edit`)
      await expect(page.getByRole('heading', { name: 'Edit Bulbasaur' })).toBeVisible()

      await page.getByLabel('Tags (comma separated)').fill('starter, not a tag!')
      await page.getByRole('button', { name: 'Save' }).click()
      await expect(page.getByText('Tags use letters, digits and hyphens (up to 30 each)')).toBeVisible()

      await page.getByLabel('Localized name').fill('Fushigidane')
      await page.getByLabel('Tags (comma separated)').fill('starter, grass')
      await page.getByRole('button', { name: 'Save' }).click()

      await expect(page).toHaveURL(/\/my-pokedex$/)
      const card = await findCard(page, 'Bulbasaur')
      await expect(card).toContainText('Fushigidane')
      await expect(card.getByRole('list', { name: 'Tags' })).toContainText('grass')
    })

    test('a stale edit shows a conflict and reloads the latest version', async ({ api, page }) => {
      await page.goto(`/my-pokedex/${BULBASAUR}/edit`)
      await expect(page.getByRole('heading', { name: 'Edit Bulbasaur' })).toBeVisible()

      await api.patchLocalPokemon(BULBASAUR, { version: bulbasaur.version, notes: 'Changed in another tab' }, session)

      await page.getByLabel('Region').fill('Johto')
      await page.getByRole('button', { name: 'Save' }).click()

      await expect(page.getByRole('alert')).toBeVisible()
      await page.getByRole('button', { name: 'Reload latest' }).click()

      await expect(page.getByLabel('Notes')).toHaveValue('Changed in another tab')
      await expect(page.getByLabel('Region')).toHaveValue(bulbasaur.region ?? '')
    })
  })
})
