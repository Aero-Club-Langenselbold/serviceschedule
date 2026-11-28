import type { Page } from '@playwright/test'
import { expect, expectPath, field, isMobile, openNavigation, test, toast } from './support/test'

const rope = (page: Page, id: string) => page.locator(`[data-rope="${id}"]`)
const year = new Date().getFullYear()

/** On phones the toasts cover the page, so they are closed before going on. */
const dismissToasts = async (page: Page) => {
  const close = page.getByRole('region', { name: /Benachrichtigungen/ }).getByRole('button', { name: /Schließen|Close/ })
  while (await close.count()) {
    await close.first().click()
  }
}

const pinFields = (page: Page) => page.locator('[data-kiosk-pin] input[data-slot="base"]')

test.describe('Windenbuch', () => {
  test('ist über das Wartungsmenü erreichbar, die Einstellungen nur für Wartungs-Admins', async ({ page, loginAs }) => {
    test.skip(isMobile(page), 'auf kleinen Bildschirmen steht der Eintrag direkt im ausgeklappten Menü')
    await loginAs('user')
    await page.goto('/')
    await openNavigation(page)
    await page.getByRole('navigation').getByRole('button', { name: 'Wartung' }).click()
    await expect(page.getByRole('link', { name: 'Windenbuch-Einstellungen' })).toHaveCount(0)
    await page.getByRole('link', { name: 'Windenbuch', exact: true }).locator('visible=true').first().click()
    await expectPath(page, '/maintenance/winch')
    await expect(page.getByRole('heading', { name: 'Windenbuch', exact: true })).toBeVisible()
  })

  test('ein Windenfahrer erfasst Vorfälle, Fotos und Bemerkungen für heute', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/maintenance/winch')
    await expect(page.locator('[data-drivers]')).toContainText('Noch niemand')
    await expect(rope(page, 'r1').locator('[data-add-incident]')).toHaveCount(5)

    // a rope break with a comment
    await rope(page, 'r1').getByRole('button', { name: 'Seilriss', exact: true }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toContainText('Seilriss an Seil 1 erfassen')
    await dialog.getByRole('textbox').fill('kurz vor dem Ausklinken')
    await dialog.getByRole('button', { name: 'Erfassen' }).click()
    await expect(toast(page, 'Seilriss erfasst.')).toBeVisible()
    await expect(rope(page, 'r1').locator('[data-incidents]')).toContainText('kurz vor dem Ausklinken')
    await expect(rope(page, 'r1').locator('[data-incidents]')).toContainText('Windenfahrer: user@example.org')
    await expect(page.locator('[data-drivers]')).toContainText('user@example.org')
    expect(api.sent('POST', '/maintenance/winch/incidents')[0]!.body).toEqual({ ropeId: 'r1', typeId: 't1', comment: 'kurz vor dem Ausklinken' })

    // any number of incidents of any kind, the comment is optional
    await rope(page, 'r1').getByRole('button', { name: 'Seilriss', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Erfassen' }).click()
    await rope(page, 'r2').getByRole('button', { name: 'Schlaufe', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Erfassen' }).click()
    await expect(rope(page, 'r1').locator('[data-incident-counts]')).toContainText('Seilriss × 2')
    await expect(rope(page, 'r2').locator('[data-incident-counts]')).toContainText('Schlaufe × 1')
    await dismissToasts(page)

    // a photo of rope 2
    await rope(page, 'r2').getByRole('button', { name: 'Foto hinzufügen' }).click()
    await page.locator('[data-photo-input]').setInputFiles({ name: 'spleiss.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgo=', 'base64') })
    await expect(rope(page, 'r2').locator('[data-photo]')).toHaveCount(1)
    expect(api.sent('POST', '/maintenance/winch/ropes/r2/photos')).toHaveLength(1)

    // remarks of the day and of a rope
    await page.locator('[data-day-remark-input]').fill('48 Starts, Winde nachgetankt')
    await page.locator('[data-save-day-remark]').click()
    await expect(page.locator('[data-day-remark]')).toContainText('Zuletzt geändert von user@example.org')
    await rope(page, 'r1').locator('[data-rope-remark-input]').fill('Seil an der Kausch neu gespleißt')
    await rope(page, 'r1').locator('[data-save-rope-remark]').click()
    await expect(rope(page, 'r1')).toContainText('Zuletzt geändert von user@example.org')

    // everything is still there after a reload
    await page.reload()
    await expect(rope(page, 'r1').locator('[data-incident]')).toHaveCount(2)
    await expect(page.locator('[data-day-remark-input]')).toHaveValue('48 Starts, Winde nachgetankt')
    await expect(rope(page, 'r1').locator('[data-rope-remark-input]')).toHaveValue('Seil an der Kausch neu gespleißt')
  })

  test('zeigt vorige Einträge nach Jahr, ältere Tage nur zum Lesen', async ({ page, api, loginAs }) => {
    api.state.winch.days[`${year - 1}-08-15`] = { remark: null, remarkVersion: 0, remarkUpdatedBy: null, ropeRemarks: {}, photos: [],
      drivers: [{ id: 'u-ben', name: 'Becker, Ben' }],
      incidents: [{ id: 'wi-last-year', ropeId: 'r2', typeId: 't4', driver: { id: 'u-ben', name: 'Becker, Ben' }, createdAt: `${year - 1}-08-15T10:00:00Z`, comment: null }] }
    await loginAs('user')
    await page.goto('/maintenance/winch')
    await page.getByRole('link', { name: 'Vorige Einträge' }).click()
    await expectPath(page, '/maintenance/winch/history')

    // the current year by default
    const days = page.locator('[data-open-day]')
    await expect(days).toHaveCount(1)
    await expect(page.locator('main')).toContainText('Seilriss × 1')
    expect(api.sent('GET', '/maintenance/winch/days')[0]!.query.get('year')).toBe(String(year))

    await page.locator('[data-year-filter]').click()
    await page.getByRole('option', { name: String(year - 1) }).click()
    await expect(page).toHaveURL(new RegExp(`year=${year - 1}`))
    await expect(page.locator(`[data-open-day="${year - 1}-08-15"]`)).toBeVisible()
    await expect(page.locator('main')).toContainText('Schlaufe × 1')

    await page.locator(`[data-open-day="${year - 1}-08-15"]`).click()
    await expectPath(page, `/maintenance/winch/days/${year - 1}-08-15`)
    await expect(rope(page, 'r2')).toContainText('Schlaufe')
    await expect(page.locator('[data-add-incident]')).toHaveCount(0)
    await expect(page.locator('[data-day-remark-input]')).toHaveCount(0)
    await expect(page.locator('[data-rope="r1"]')).toHaveCount(0)
  })

  test('Wartungs-Admins legen Seile und Vorfälle fest und korrigieren ältere Tage', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/maintenance/winch')
    await page.getByRole('link', { name: 'Windenbuch-Einstellungen' }).click()
    await expectPath(page, '/maintenance/winch/settings')

    const ropes = page.locator('[data-option-list="ropes"]')
    await ropes.locator('[data-new-option]').fill('Seil 3')
    await ropes.getByRole('button', { name: 'Hinzufügen' }).click()
    await expect(ropes.locator('[data-option-name]')).toHaveCount(3)
    await ropes.locator('[data-new-option]').fill('seil 3')
    await ropes.getByRole('button', { name: 'Hinzufügen' }).click()
    await expect(toast(page, 'Diese Bezeichnung gibt es schon.')).toBeVisible()

    const types = page.locator('[data-option-list="incident-types"]')
    await types.locator('[data-new-option]').fill('Seilverschleiß')
    await types.getByRole('button', { name: 'Hinzufügen' }).click()
    await expect(types.locator('[data-option-name]')).toHaveCount(6)

    // a used incident type is only taken out of use
    await types.locator('[data-option="t1"]').getByRole('button', { name: 'Nicht mehr verwenden' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Nicht mehr verwenden' }).click()
    await expect(types.locator('[data-option="t1"] [data-inactive]')).toBeVisible()
    expect(api.state.winch.incidentTypes.find((type: any) => type.id === 't1').active).toBe(false)

    await page.goto('/maintenance/winch')
    await expect(rope(page, 'ropes-3')).toContainText('Seil 3')
    await expect(rope(page, 'r1').getByRole('button', { name: 'Seilverschleiß' })).toBeVisible()
    await expect(rope(page, 'r1').getByRole('button', { name: 'Seilriss', exact: true })).toHaveCount(0)

    // earlier days: correct the remark, delete an incident
    await page.goto(`/maintenance/winch/days/${year}-05-01`)
    await expect(page.locator('[data-day-remark-input]')).toHaveValue('Böiger Westwind')
    await page.locator('[data-day-remark-input]').fill('Böiger Westwind, 25 Starts')
    await page.locator('[data-save-day-remark]').click()
    await expect(page.locator('[data-day-remark]')).toContainText('Zuletzt geändert von admin@example.org')
    await rope(page, 'r1').getByRole('button', { name: 'Vorfall löschen' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click()
    // an earlier day only shows the ropes it has entries of
    await expect(rope(page, 'r1')).toHaveCount(0)
    await expect(page.locator('[data-no-ropes]')).toContainText('An diesem Tag wurde zu keinem Seil etwas eingetragen.')
    await expect(page.locator('[data-day-remark-input]')).toHaveValue('Böiger Westwind, 25 Starts')
  })

  test('ist im Kiosk-Modus benutzbar, ohne Einstellungen', async ({ page, api }) => {
    await page.goto('/login')
    await page.getByRole('tab', { name: 'Kiosk' }).click()
    await field(page, 'E-Mail-Adresse').fill('admin@example.org')
    await pinFields(page).first().click()
    await page.keyboard.type('123456')
    await expectPath(page, '/kiosk')

    await page.locator('[data-kiosk-tiles]').getByRole('link', { name: /Windenbuch/ }).click()
    await expectPath(page, '/maintenance/winch')
    await expect(page.locator('[data-settings-link]')).toHaveCount(0)

    await rope(page, 'r2').getByRole('button', { name: 'Vorseilriss' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Erfassen' }).click()
    await expect(rope(page, 'r2').locator('[data-incident]')).toHaveCount(1)
    expect(api.sent('POST', '/maintenance/winch/incidents')).toHaveLength(1)

    // the earlier entries are open, the settings are not
    await page.getByRole('link', { name: 'Vorige Einträge' }).click()
    await expectPath(page, '/maintenance/winch/history')
    await page.goto('/maintenance/winch/settings')
    await expectPath(page, '/kiosk')
  })
})
