import { expect, test } from './fixtures'

test.describe('smoke', () => {
  test('the catalog loads through the nginx proxy without private links for a visitor', async ({ page }) => {
    await page.goto('/')

    await expect(page.getByRole('heading', { name: 'Pokemon catalog' })).toBeVisible()
    await expect(page.getByRole('link', { name: /bulbasaur/i })).toBeVisible()
    await expect(page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'My Pokedex' })).toHaveCount(0)
  })

  test('a protected page sends a visitor to the login form', async ({ page }) => {
    await page.goto('/my-pokedex')

    await expect(page).toHaveURL(/\/login\?redirect=%2Fmy-pokedex/)
    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible()
  })
})
