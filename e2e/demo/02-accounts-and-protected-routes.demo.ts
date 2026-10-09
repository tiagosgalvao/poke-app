import type { Page } from '@playwright/test'
import { ASH, expect, test } from '../tests/fixtures'
import { Narrator } from './narrator'

function mainNav(page: Page) {
  return page.getByRole('navigation', { name: 'Main' })
}

test('chapter 2: accounts and protected routes', async ({ page }) => {
  const narrator = await Narrator.attach(page)
  await page.goto('/')
  await expect(page.getByRole('link', { name: /bulbasaur/i })).toBeVisible()

  await narrator.say('Chapter 2 · Accounts and protected routes.')
  await narrator.say('A visitor only sees the public Catalog in the menu.', mainNav(page))

  await narrator.say('What if a visitor opens /my-pokedex directly?')
  await page.goto('/my-pokedex')
  await expect(page).toHaveURL(/\/login\?redirect=%2Fmy-pokedex$/)
  const form = page.locator('form')
  await narrator.say('Protected pages send visitors to log in, and remember where they were going.', form)

  await narrator.type(page.getByLabel('Username'), ASH.username, 'First, the demo user with a wrong password.')
  await narrator.type(page.getByLabel('Password'), 'not-the-password')
  await narrator.click(page.getByRole('button', { name: 'Log in' }))
  await expect(page.getByRole('alert')).toBeVisible()
  await narrator.say(
    "The API answers 401 and the form shows its message, without saying which half was wrong.",
    page.getByRole('alert'),
  )

  await narrator.type(page.getByLabel('Password'), ASH.password, 'Now the right password.')
  await narrator.click(page.getByRole('button', { name: 'Log in' }))
  await expect(page).toHaveURL(/\/my-pokedex$/)
  await narrator.say(
    'Back on My Pokedex. The menu now shows it, plus who is signed in. A JWT is kept in the browser and sent with every API call.',
    page.locator('header'),
  )

  await narrator.click(page.getByRole('button', { name: 'Log out' }), 'Logging out returns to the catalog…')
  await expect(page).toHaveURL(/\/$/)
  await expect(mainNav(page).getByRole('link', { name: 'My Pokedex' })).toHaveCount(0)
  await narrator.say('…and the private link is gone again.', mainNav(page))

  const username = `demo-${Date.now()}`
  await narrator.click(page.getByRole('link', { name: 'Sign up' }), 'Anyone can create an account.')
  await narrator.type(page.getByLabel('Username'), username)
  await narrator.type(page.getByLabel('Email'), `${username}@example.com`)
  await narrator.type(page.getByLabel('Password'), 'Demo-Pass-123!', 'Passwords are hashed with BCrypt on the server.')
  await narrator.click(page.getByRole('button', { name: 'Create account' }))
  await expect(page).toHaveURL(/\/my-pokedex$/)
  await narrator.say('New accounts are signed in straight away.', page.getByText(`Signed in as ${username}`))
  await narrator.say('Next: My Pokedex, the local copy we can sync and edit.')
})
