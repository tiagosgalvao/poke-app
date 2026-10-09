import { ASH, expect, test } from './fixtures'
import type { Page } from '@playwright/test'

function myPokedexLink(page: Page) {
  return page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'My Pokedex' })
}

test.describe('auth', () => {
  test('a visitor is sent to log in and comes back to My Pokedex', async ({ page }) => {
    await page.goto('/')
    await expect(myPokedexLink(page)).toHaveCount(0)

    await page.goto('/my-pokedex')
    await expect(page).toHaveURL(/\/login\?redirect=%2Fmy-pokedex$/)

    await page.getByLabel('Username').fill(ASH.username)
    await page.getByLabel('Password').fill(ASH.password)
    await page.getByRole('button', { name: 'Log in' }).click()

    await expect(page).toHaveURL(/\/my-pokedex$/)
    await expect(page.getByRole('heading', { name: 'My Pokedex' })).toBeVisible()
    await expect(myPokedexLink(page)).toBeVisible()
    await expect(page.getByText(`Signed in as ${ASH.username}`)).toBeVisible()

    await page.getByRole('button', { name: 'Log out' }).click()

    await expect(page).toHaveURL(/\/$/)
    await expect(myPokedexLink(page)).toHaveCount(0)
  })

  test('a wrong password is refused', async ({ page }) => {
    await page.goto('/login')

    await page.getByLabel('Username').fill(ASH.username)
    await page.getByLabel('Password').fill('not-the-password')
    await page.getByRole('button', { name: 'Log in' }).click()

    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('a new user registers and is signed in', async ({ page }) => {
    const username = `e2e-${Date.now()}`
    await page.goto('/register')

    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Email').fill(`${username}@example.com`)
    await page.getByLabel('Password').fill('Demo-Pass-123!')
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page).toHaveURL(/\/my-pokedex$/)
    await expect(page.getByText(`Signed in as ${username}`)).toBeVisible()
  })
})
