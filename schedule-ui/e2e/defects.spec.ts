import type { Page } from '@playwright/test'
import { expect, expectPath, isMobile, openNavigation, test, toast } from './support/test'

const bell = (page: Page) => page.locator('[data-notification-bell]')
const dialog = (page: Page) => page.getByRole('dialog')
const entry = (page: Page, title: string) => page.locator('[data-defect]', { hasText: title })

test.describe('Benachrichtigungen', () => {
  test('zeigen zugewiesene Mängel am Benutzernamen und springen zum Mangel', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await expect(bell(page)).toHaveAttribute('aria-label', /1 ungelesene/)

    await bell(page).click()
    await page.getByRole('button', { name: /admin@example.org hat dir einen Mangel zugewiesen/ }).click()

    await expect(page).toHaveURL(/\/maintenance\/defects\?defect=d1$/)
    await expect(dialog(page)).toContainText('Riss in der Haube')
    await expect(dialog(page)).toContainText('User, Uta')
    await expect(bell(page)).toHaveAttribute('aria-label', /Keine ungelesenen/)
    expect(api.sent('PUT', '/notifications/n1/read')).toHaveLength(1)
  })

  test('lassen sich alle auf einmal als gelesen markieren', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await bell(page).click()
    await page.getByRole('button', { name: 'Alle als gelesen markieren' }).click()

    await expect(bell(page)).toHaveAttribute('aria-label', /Keine ungelesenen/)
    expect(api.sent('PUT', '/notifications/read-all')).toHaveLength(1)
  })
})

test.describe('Mängelbuch', () => {
  test('ist über das Wartungsmenü erreichbar', async ({ page, loginAs }) => {
    test.skip(isMobile(page), 'auf kleinen Bildschirmen steht der Eintrag direkt im ausgeklappten Menü')
    await loginAs('user')
    await page.goto('/')
    await openNavigation(page)
    await page.getByRole('navigation').getByRole('button', { name: 'Wartung' }).click()
    await page.getByRole('link', { name: 'Mängelbuch' }).locator('visible=true').first().click()
    await expectPath(page, '/maintenance/defects')
  })

  test('filtert nach Flugzeug oder Gerät und Status', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/maintenance/defects')
    await expect(entry(page, 'Riss in der Haube')).toBeVisible()
    await expect(entry(page, 'Seil ausgefranst')).toBeVisible()
    await expect(entry(page, 'Reifen platt')).toHaveCount(0)

    await page.locator('[data-aircraft-filter]').click()
    await page.getByRole('option', { name: 'Winde 1 · Winde' }).click()
    await expect(page).toHaveURL(/aircraft=w1/)
    await expect(entry(page, 'Seil ausgefranst')).toBeVisible()
    await expect(entry(page, 'Riss in der Haube')).toHaveCount(0)

    await page.goto('/maintenance/defects?status=CLOSED')
    await expect(entry(page, 'Reifen platt')).toBeVisible()
    await expect(entry(page, 'Seil ausgefranst')).toHaveCount(0)
  })

  test('Mitglieder arbeiten Mängel ab, Wartungs-Admins nehmen sie ab', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/maintenance/defects?defect=d2')
    await expect(dialog(page)).toContainText('In Bearbeitung')
    await expect(dialog(page).getByRole('button', { name: 'Abnehmen' })).toHaveCount(0)

    await dialog(page).getByRole('textbox', { name: 'Kommentieren' }).fill('Neues Seil ist bestellt')
    await dialog(page).getByRole('button', { name: 'Kommentieren' }).click()
    await expect(dialog(page)).toContainText('Neues Seil ist bestellt')

    await dialog(page).getByRole('button', { name: 'Als erledigt melden' }).click()
    await expect(dialog(page).locator('[data-defect-status]')).toHaveText('Erledigt')
    await expect(dialog(page).getByRole('button', { name: 'Abnehmen' })).toHaveCount(0)
    expect(api.sent('PUT', '/maintenance/defects/d2/status')[0]!.body).toEqual({ status: 'DONE', comment: null })
  })

  test('Administratoren nehmen erledigte Mängel ab, danach sind sie gesperrt', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/maintenance/defects?defect=d3')
    await dialog(page).getByRole('button', { name: 'Abnehmen' }).click()

    await expect(dialog(page).locator('[data-defect-status]')).toHaveText('Abgenommen')
    await expect(dialog(page)).toContainText('Abgenommene Mängel sind gesperrt')
    await expect(dialog(page).getByRole('textbox', { name: 'Kommentieren' })).toHaveCount(0)
    await expect(dialog(page).getByRole('button', { name: 'Abnahme zurücknehmen' })).toBeVisible()
  })

  test('Mitglieder können Mängel nicht abweisen', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/maintenance/defects?defect=d1')
    await expect(dialog(page).getByRole('button', { name: 'Als erledigt melden' })).toBeVisible()
    await expect(dialog(page).getByRole('button', { name: 'Abweisen' })).toHaveCount(0)
  })

  test('Administratoren weisen Mängel nur mit Begründung ab', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/maintenance/defects?defect=d1')
    await dialog(page).getByRole('button', { name: 'Abweisen' }).click()

    const form = dialog(page).locator('[data-reject-form]')
    await expect(form.getByRole('button', { name: 'Abweisen' })).toBeDisabled()
    await form.getByRole('textbox').fill('Nur ein Schmutzfleck')
    await form.getByRole('button', { name: 'Abweisen' }).click()

    await expect(dialog(page).locator('[data-defect-status]')).toHaveText('Abgewiesen')
    await expect(dialog(page)).toContainText('Nur ein Schmutzfleck')
    expect(api.sent('PUT', '/maintenance/defects/d1/status')[0]!.body).toEqual({ status: 'REJECTED', comment: 'Nur ein Schmutzfleck' })
  })

  test('Administratoren melden Mängel an der Winde und weisen sie Benutzern zu', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/maintenance/defects?aircraft=w1')
    await page.getByRole('button', { name: 'Mangel melden' }).click()

    await dialog(page).getByRole('textbox', { name: 'Titel' }).fill('Bremse der Winde schleift')
    await dialog(page).locator('[data-assignee-select]').click()
    await page.getByRole('option', { name: 'Becker, Ben' }).click()
    await page.keyboard.press('Escape')
    await dialog(page).getByRole('button', { name: 'Speichern' }).click()

    await expect(toast(page, 'Gespeichert')).toBeVisible()
    await expect(dialog(page)).toContainText('Bremse der Winde schleift')
    await expect(dialog(page).locator('[data-assignees]')).toHaveText('Becker, Ben')
    expect(api.sent('POST', '/maintenance/aircraft/w1/defects')[0]!.body).toMatchObject({ title: 'Bremse der Winde schleift', assigneeIds: ['u-ben'] })
  })
})
