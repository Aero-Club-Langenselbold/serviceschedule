import { expect, test } from '@playwright/test'

const BASE_URL = 'http://localhost:3000'

test('unauthenticated user is redirected to /login when accessing protected route', async ({ page }) => {
    // Clear any stored session by going to login page first
    await page.goto(`${BASE_URL}/login`)
    const logoutBtn = page.getByRole('button', { name: 'Logout' })
    if (await logoutBtn.isVisible()) {
        await logoutBtn.click()
    }

    await page.goto(`${BASE_URL}/plan/list`)
    await expect(page).toHaveURL(`${BASE_URL}/login`)
})

test('unauthenticated user is redirected to /login when accessing participants', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    const logoutBtn = page.getByRole('button', { name: 'Logout' })
    if (await logoutBtn.isVisible()) {
        await logoutBtn.click()
    }

    await page.goto(`${BASE_URL}/participants/list`)
    await expect(page).toHaveURL(`${BASE_URL}/login`)
})

test('logged in user can access plan list', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.getByRole('textbox', { name: /benutzername/i }).fill('c.triebstein@arcor.de')
    await page.getByRole('textbox', { name: /passwort/i }).fill('password')
    await page.getByRole('button', { name: /anmelden/i }).click()

    await expect(page.getByRole('button', { name: 'c.triebstein@arcor.de' })).toBeVisible()
    await page.goto(`${BASE_URL}/plan/list`)
    await expect(page).toHaveURL(`${BASE_URL}/plan/list`)
})

test('login page does not redirect away from itself for unauthenticated users', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await expect(page).toHaveURL(`${BASE_URL}/login`)
    // Login form should be visible
    await expect(page.getByRole('textbox').first()).toBeVisible()
})

test('can navigate to permission-denied page when logged in', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.getByRole('textbox', { name: /benutzername/i }).fill('c.triebstein@arcor.de')
    await page.getByRole('textbox', { name: /passwort/i }).fill('password')
    await page.getByRole('button', { name: /anmelden/i }).click()

    await expect(page.getByRole('button', { name: 'c.triebstein@arcor.de' })).toBeVisible()
    await page.goto(`${BASE_URL}/permission-denied`)
    await expect(page).toHaveURL(`${BASE_URL}/permission-denied`)
})
