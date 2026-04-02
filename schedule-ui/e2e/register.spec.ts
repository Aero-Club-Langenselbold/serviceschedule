import { expect, test } from '@playwright/test'

const BASE_URL = 'http://localhost:3000'

test('register page shows the full registration form', async ({ page }) => {
    await page.goto(`${BASE_URL}/register`)

    await expect(page.getByRole('heading')).toBeVisible()
    // E-Mail field
    await expect(page.getByRole('textbox').first()).toBeVisible()
    // Password field
    await expect(page.getByRole('textbox').nth(1)).toBeVisible()
    // Registration Key field
    await expect(page.locator('input[type="text"]').last()).toBeVisible()
})

test('register button is disabled when form is empty', async ({ page }) => {
    await page.goto(`${BASE_URL}/register`)

    // The submit button should be disabled when all fields are empty
    const submitButton = page.getByRole('button').filter({ hasText: /register/i })
    await expect(submitButton).toBeDisabled()
})

test('register button stays disabled when passwords do not match', async ({ page }) => {
    await page.goto(`${BASE_URL}/register`)

    const inputs = page.getByRole('textbox')
    await inputs.nth(0).fill('test@example.com')
    await inputs.nth(1).fill('Password123!')
    await inputs.nth(2).fill('DifferentPassword1!')

    const submitButton = page.getByRole('button').filter({ hasText: /register/i })
    await expect(submitButton).toBeDisabled()
})

test('navigating to register from login works', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page).toHaveURL(`${BASE_URL}/register`)
})
