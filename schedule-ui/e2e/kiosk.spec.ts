import type { Page } from '@playwright/test'
import { expect, expectPath, field, test } from './support/test'

const pinFields = (page: Page) => page.locator('[data-kiosk-pin] input[data-slot="base"]')

const chooseKiosk = (page: Page) => page.getByRole('tab', { name: 'Kiosk' }).click()

const signInKiosk = async (page: Page, username: string, pin: string) => {
  await page.goto('/login')
  await chooseKiosk(page)
  await field(page, 'E-Mail-Adresse').fill(username)
  await pinFields(page).first().click()
  await page.keyboard.type(pin)
}

test.describe('Kiosk-Modus', () => {
  test('ersetzt das Passwort durch die PIN und blendet alle anderen Felder aus, auch mit Vereinsflieger-Anmeldung', async ({ page, api }) => {
    api.state.features = [
      { id: 'vfLoginFeature', description: '', active: true },
      { id: 'securityQuestionFeature', description: '', active: true },
    ]
    await page.goto('/login')
    await expect(field(page, 'Sicherheitscode (bei aktivierter 2FA)')).toBeVisible()

    await chooseKiosk(page)
    await expect(field(page, 'E-Mail-Adresse')).toBeVisible()
    await expect(pinFields(page)).toHaveCount(6)
    await expect(page.getByLabel('Passwort', { exact: true })).toHaveCount(0)
    await expect(field(page, 'Sicherheitscode (bei aktivierter 2FA)')).toHaveCount(0)
    await expect(field(page, 'Antwort auf Sicherheitsfrage')).toHaveCount(0)
  })

  test('merkt sich die Auswahl auf dem Gerät', async ({ page }) => {
    await page.goto('/login')
    await chooseKiosk(page)
    await page.reload()
    await expect(pinFields(page)).toHaveCount(6)

    await page.getByRole('tab', { name: 'Regulär' }).click()
    await page.reload()
    await expect(page.getByLabel('Passwort', { exact: true })).toBeVisible()
  })

  test('meldet sich mit E-Mail und PIN an und zeigt nur Wartung und Betrieb', async ({ page, api }) => {
    await signInKiosk(page, 'admin@example.org', '123456')

    // the PIN submits by itself once complete
    await expectPath(page, '/kiosk')
    expect(api.sent('POST', '/auth/kiosk/signin')[0]!.body).toEqual({ username: 'admin@example.org', pin: '123456' })
    const header = page.locator('[data-kiosk-header]')
    await expect(header.getByRole('link', { name: 'Checklisten' })).toBeVisible()
    await expect(header.getByRole('link', { name: 'Mängelbuch' })).toBeVisible()
    // no bug list, notifications, personal settings or administration, although this user is an administrator
    await expect(page.locator('a[href="/request/list"]')).toHaveCount(0)
    await expect(page.locator('a[href="/users/list"]')).toHaveCount(0)
    await expect(page.locator('a[href="/profile"]')).toHaveCount(0)
    await expect(page.locator('[data-notification-bell]')).toHaveCount(0)

    await page.locator('[data-kiosk-tiles]').getByRole('link', { name: /Checklisten/ }).click()
    await expectPath(page, '/operations')
    await expect(page.locator('[data-aircraft="a1"]')).toBeVisible()
  })

  test('führt von allen anderen Seiten zurück zur Kiosk-Startseite', async ({ page }) => {
    await signInKiosk(page, 'user@example.org', '123456')
    await expectPath(page, '/kiosk')

    for (const path of ['/request/list', '/profile', '/', '/plan/mine', '/maintenance/templates']) {
      await page.goto(path)
      await expectPath(page, '/kiosk')
    }
    await page.goto('/maintenance/defects')
    await expectPath(page, '/maintenance/defects')
  })

  test('lehnt falsche PINs und Konten ohne PIN ab', async ({ page }) => {
    await signInKiosk(page, 'user@example.org', '000000')
    await expect(page.getByText('E-Mail-Adresse oder PIN falsch')).toBeVisible()
    await expectPath(page, '/login')

    await field(page, 'E-Mail-Adresse').fill('nobody@example.org')
    await pinFields(page).first().click()
    await page.keyboard.type('123456')
    await expect(page.getByText('keine Kiosk-PIN festgelegt')).toBeVisible()
  })

  test('meldet gesperrte Konten', async ({ page }) => {
    await signInKiosk(page, 'user@example.org', '999999')
    await expect(page.getByText('für 15 Minuten gesperrt')).toBeVisible()
  })

  test('meldet sich über die Schaltfläche ab', async ({ page }) => {
    await signInKiosk(page, 'user@example.org', '123456')
    await expectPath(page, '/kiosk')
    await page.locator('[data-kiosk-logout]').click()
    await expectPath(page, '/login')
    // the tablet stays in kiosk mode
    await expect(pinFields(page)).toHaveCount(6)
  })
})
