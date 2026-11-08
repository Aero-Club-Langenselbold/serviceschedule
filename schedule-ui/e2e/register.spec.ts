import type { Page } from '@playwright/test'
import { expect, expectPath, field, test } from './support/test'

const submit = (page: Page) => page.locator('button[type="submit"]')

const fillForm = async (page: Page, { password = 'Passwort1', confirm = password } = {}) => {
  await field(page, 'E-Mail-Adresse (wie vom Administrator bereitgestellt)').fill('neu@example.org')
  await field(page, 'Passwort').fill(password)
  await field(page, 'Passwort bestätigen').fill(confirm)
  await field(page, 'Registrierungsschlüssel').fill('KEY-1')
}

test.describe('Registrierung', () => {
  test('ist von der Anmeldung aus erreichbar', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: 'Registrieren' }).click()
    await expectPath(page, '/register')
  })

  test('hält das Absenden gesperrt, solange das Formular leer ist', async ({ page }) => {
    await page.goto('/register')
    await expect(submit(page)).toBeDisabled()
  })

  test('meldet nicht übereinstimmende Passwörter', async ({ page }) => {
    await page.goto('/register')
    await fillForm(page, { confirm: 'Anders123' })
    await expect(page.getByText('Passwörter stimmen nicht überein')).toBeVisible()
    await expect(submit(page)).toBeDisabled()
  })

  test('bewertet die Passwortstärke', async ({ page }) => {
    await page.goto('/register')
    const password = field(page, 'Passwort')

    await expect(page.getByText('Gib ein Passwort ein')).toBeVisible()
    await password.fill('abc')
    await expect(page.getByText('Schwaches Passwort')).toBeVisible()
    await password.fill('abcdefg1')
    await expect(page.getByText('Mittleres Passwort')).toBeVisible()
    await password.fill('Abcdefg1')
    await expect(page.getByText('Starkes Passwort')).toBeVisible()
  })

  test('lässt schwache Passwörter nicht zu', async ({ page }) => {
    await page.goto('/register')
    await fillForm(page, { password: 'schwach' })
    await expect(submit(page)).toBeDisabled()
  })

  test('registriert mit gültigen Daten', async ({ page, api }) => {
    await page.goto('/register')
    await fillForm(page)
    await submit(page).click()

    await expect(page.getByText('Registrierung erfolgreich')).toBeVisible()
    const body = api.sent('POST', '/auth/signup')[0]!.body
    expect(body).toMatchObject({ username: 'neu@example.org', password: 'Passwort1', registrationKey: 'KEY-1' })
  })

  test('zeigt die Fehlermeldung des Servers', async ({ page, api }) => {
    api.fail('POST', /^\/auth\/signup$/, 400, { message: 'Schlüssel ungültig' })
    await page.goto('/register')
    await fillForm(page)
    await submit(page).click()
    await expect(page.getByText('Schlüssel ungültig')).toBeVisible()
  })
})
