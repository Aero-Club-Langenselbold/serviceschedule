import type { Page } from '@playwright/test'
import { expect, field, test } from './support/test'

const row = (page: Page, text: string) => page.locator('tr', { hasText: text })

test.describe('Dienst anlegen', () => {
  test('speichert einen Dienst mit Einstellung', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/service/add')

    await field(page, 'Dienst').fill('Nachtdienst')
    await page.getByRole('spinbutton', { name: 'Teilnehmer' }).fill('3')
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('Dienst erfolgreich gespeichert')).toBeVisible()
    const body = api.sent('POST', '/service/save')[0]!.body
    expect(body.servicename).toBe('Nachtdienst')
    expect(body.settings).toHaveLength(1)
    expect(Number(body.settings[0].mandatoryAssignees)).toBe(3)
  })

  test('fügt Einstellungen hinzu und entfernt sie', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/service/add')
    const remove = page.getByRole('button', { name: 'Einstellung entfernen' })

    await expect(remove).toHaveCount(1)
    await page.getByRole('button', { name: 'Einstellung hinzufügen' }).click()
    await expect(remove).toHaveCount(2)
    await remove.first().click()
    await expect(remove).toHaveCount(1)
  })

  test('meldet bereits vorhandene Dienste', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    api.fail('POST', /^\/service\/save$/, 409)
    await page.goto('/service/add')
    await field(page, 'Dienst').fill('Frühdienst')
    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Dienst existiert bereits')).toBeVisible()
  })
})

test.describe('Dienstliste', () => {
  const details = (page: Page, service: string) => row(page, service).getByRole('button', { name: 'Details ein-/ausblenden' })

  test('klappt die Einstellungen eines Dienstes auf', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/service/list')
    await expect(row(page, 'Frühdienst')).toBeVisible()
    await expect(page.getByText('09:00')).toHaveCount(0)

    await details(page, 'Frühdienst').click()
    await expect(page.getByText('09:00')).toBeVisible()
    await expect(page.getByText('17:00')).toBeVisible()
    await expect(page.getByText('Januar')).toBeVisible()
    await expect(page.getByText('Dezember')).toBeVisible()
  })

  test('bearbeitet einen Dienst', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/service/list')

    await row(page, 'Frühdienst').getByRole('button', { name: 'Bearbeiten' }).click()
    const editing = page.locator('tr').filter({ has: page.getByRole('button', { name: 'Speichern' }) })
    await editing.locator('input[type="text"], input:not([type])').first().fill('Morgendienst')
    await editing.getByRole('button', { name: 'Speichern' }).click()

    await expect(row(page, 'Morgendienst')).toBeVisible()
    expect(api.sent('POST', '/service/save')[0]!.body).toMatchObject({ id: 's1', servicename: 'Morgendienst' })
  })

  test('löscht einen Dienst', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/service/list')

    await row(page, 'Spätdienst').getByRole('button', { name: 'Löschen' }).click()
    await expect(row(page, 'Spätdienst')).toHaveCount(0)
    expect(api.sent('DELETE', '/service/delete/s2')).toHaveLength(1)
  })

  test('nimmt Einstellungen beim Bearbeiten auf', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/service/list')

    await details(page, 'Frühdienst').click()
    await row(page, 'Frühdienst').first().getByRole('button', { name: 'Bearbeiten' }).click()
    await page.getByRole('button', { name: 'Einstellung hinzufügen' }).click()
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect.poll(() => api.sent('POST', '/service/save').length).toBe(1)
    expect(api.sent('POST', '/service/save')[0]!.body.settings).toHaveLength(2)
  })
})
