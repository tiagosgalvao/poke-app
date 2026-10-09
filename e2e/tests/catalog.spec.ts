import { expect, test } from './fixtures'

test.describe('catalog', () => {
  test('browse the catalog page by page', async ({ page }) => {
    await page.goto('/')

    const bulbasaur = page.getByRole('link', { name: /bulbasaur/i })
    await expect(bulbasaur).toContainText('Seed Pokémon')
    await expect(bulbasaur).toContainText('6.9 kg')
    await expect(bulbasaur.getByRole('list', { name: 'Abilities' })).toContainText('overgrow')
    await expect(page.getByText(/^Page 1 of \d+$/)).toBeVisible()

    await page.getByRole('button', { name: 'Next' }).click()

    await expect(page).toHaveURL(/\?page=2$/)
    await expect(page.getByText(/^Page 2 of \d+$/)).toBeVisible()
    await expect(page.getByRole('link', { name: /spearow/i })).toBeVisible()
  })

  test('open a Pokemon with its stats, description and evolution chain', async ({ page }) => {
    await page.goto('/pokemon/eevee')

    await expect(page.getByRole('heading', { name: 'Eevee', level: 2 })).toBeVisible()
    await expect(page.getByText('Evolution Pokémon')).toBeVisible()
    await expect(page.getByRole('progressbar')).toHaveCount(6)
    await expect(page.getByRole('progressbar', { name: 'hp' })).toHaveAttribute('aria-valuenow', '55')

    const chain = page.getByRole('list', { name: 'Evolution chain' })
    await expect(chain.getByRole('link', { name: /eevee/i })).toHaveAttribute('aria-current', 'page')
    await expect(chain.getByRole('link')).toHaveCount(9)

    await chain.getByRole('link', { name: /vaporeon/i }).click()

    await expect(page).toHaveURL(/\/pokemon\/vaporeon$/)
    await expect(page.getByRole('heading', { name: 'Vaporeon', level: 2 })).toBeVisible()
  })
})
