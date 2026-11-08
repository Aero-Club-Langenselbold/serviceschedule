import { expect, test } from './support/test'

test.describe('Einstellungen', () => {
  test('lädt Land, Bundesland und allgemeine Einstellungen', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/settings')

    await expect(page.getByRole('combobox').first()).toContainText('Deutschland')
    await expect(page.getByRole('combobox').nth(1)).toContainText('Berlin')
    await expect(page.getByText('Minimale Anzahl an verfügbaren Tagen')).toBeVisible()
    await expect(page.getByText('Sicherheitsfrage')).toBeVisible()
    await expect(page.locator('input').first()).toHaveValue('3')
  })

  test('speichert Änderungen', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/settings')
    await expect(page.locator('input').first()).toHaveValue('3')

    await page.locator('input').first().fill('5')
    await page.getByRole('combobox').nth(1).click()
    await page.getByRole('option', { name: 'Bayern' }).click()
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('Einstellungen erfolgreich gespeichert')).toBeVisible()
    expect(api.sent('POST', '/settings/common-settings')[0]!.body[0]).toMatchObject({ settingKey: 'min_participation_count', settingValue: '5' })
    expect(api.sent('POST', '/settings/selected-state')[0]!.body).toEqual({ country: { id: 'de' }, state: { id: 'by' } })
  })

  test('meldet Fehler beim Speichern', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    api.fail('POST', /^\/settings\/common-settings$/)
    await page.goto('/settings')
    await expect(page.locator('input').first()).toHaveValue('3')
    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('An error occurred while saving common settings.')).toBeVisible()
  })
})
