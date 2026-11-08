import type { Page } from '@playwright/test'
import { expect, field, test, toast } from './support/test'

const row = (page: Page, text: string) => page.locator('tr', { hasText: text })

test.describe('Bug- und Feature-Liste', () => {
  test('listet Einträge mit Status, Typ und Autor', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/request/list')

    await expect(row(page, 'Kalender hakt')).toContainText('Offen')
    await expect(row(page, 'Kalender hakt')).toContainText('Bug')
    await expect(row(page, 'Kalender hakt')).toContainText('Anna Albers')
    await expect(row(page, 'Kalender hakt')).toContainText('01.11.2026')
    await expect(row(page, 'Dunkles Logo')).toContainText('In Bearbeitung')
    await expect(row(page, 'Dunkles Logo')).toContainText('Feature')
  })

  test('klappt die Beschreibung auf', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/request/list')

    await row(page, 'Kalender hakt').getByRole('button', { name: 'Details ein-/ausblenden' }).click()
    await expect(page.getByText('Der Kalender reagiert nicht.')).toBeVisible()
  })

  test('legt einen neuen Eintrag an', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/request/list')
    await expect(row(page, 'Kalender hakt')).toBeVisible()

    await page.getByRole('button', { name: 'Neu erstellen' }).click()
    const dialog = page.getByRole('dialog')
    const save = dialog.getByRole('button', { name: 'Speichern' })
    await expect(save).toBeDisabled()

    await field(page, 'Thema').fill('Neuer Fehler')
    await dialog.getByRole('textbox', { name: 'Beschreibung' }).fill('Geht nicht.')
    await dialog.getByRole('radio', { name: /Feature/ }).click()
    await save.click()

    await expect(toast(page, 'Bug/Feature erfolgreich erstellt')).toBeVisible()
    await expect(row(page, 'Neuer Fehler')).toBeVisible()
    expect(api.sent('POST', '/request/save')[0]!.body).toMatchObject({
      headline: 'Neuer Fehler', description: 'Geht nicht.', type: 'FEATURE', status: 'OPEN', author: { id: 'p3' },
    })
  })

  test('bearbeitet einen Eintrag', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/request/list')

    await row(page, 'Kalender hakt').getByRole('button', { name: 'Bearbeiten' }).click()
    const dialog = page.getByRole('dialog')
    await expect(field(page, 'Thema')).toHaveValue('Kalender hakt')
    await dialog.getByRole('combobox').click()
    await page.getByRole('option', { name: 'Geschlossen' }).click()
    await dialog.getByRole('button', { name: 'Speichern' }).click()

    await expect(row(page, 'Kalender hakt')).toContainText('Geschlossen')
    expect(api.sent('POST', '/request/save')[0]!.body).toMatchObject({ id: 'r1', status: 'CLOSED' })
  })

  test('verwirft Eingaben beim Abbrechen', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/request/list')
    await expect(row(page, 'Kalender hakt')).toBeVisible()

    await page.getByRole('button', { name: 'Neu erstellen' }).click()
    await field(page, 'Thema').fill('Doch nicht')
    await page.getByRole('dialog').getByRole('button', { name: 'Abbrechen' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    expect(api.sent('POST', '/request/save')).toHaveLength(0)
  })

  test('meldet Fehler beim Speichern', async ({ page, api, loginAs }) => {
    await loginAs('user')
    api.fail('POST', /^\/request\/save$/)
    await page.goto('/request/list')
    await expect(row(page, 'Kalender hakt')).toBeVisible()

    await page.getByRole('button', { name: 'Neu erstellen' }).click()
    await field(page, 'Thema').fill('Neu')
    await page.getByRole('dialog').getByRole('textbox', { name: 'Beschreibung' }).fill('Text')
    await page.getByRole('dialog').getByRole('button', { name: 'Speichern' }).click()
    await expect(toast(page, 'Fehler beim Erstellen')).toBeVisible()
  })
})
