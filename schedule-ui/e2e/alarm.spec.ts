import type { Page } from '@playwright/test'
import { expect, test } from './support/test'

const row = (page: Page, text: string) => page.locator('tr', { hasText: text })

test.describe('Ereignisprotokoll', () => {
  test('beschreibt RFID- und Statusereignisse', async ({ page, loginAs }) => {
    await loginAs('alarm')
    await page.goto('/alarm/list')

    await expect(row(page, 'AABBCC')).toContainText('RFID-Leser: Schlüssel erkannt')
    await expect(row(page, 'AABBCC')).toContainText('Autorisierter Schlüssel: AABBCC')
    await expect(row(page, 'DDEEFF')).toContainText('Nicht autorisierter Schlüssel: DDEEFF')
    await expect(row(page, 'Anlage Scharf')).toContainText('Alarmanlage: Statusänderung')
    await expect(row(page, 'Anlage Scharf')).toContainText('Zone 2')
  })

  test('zeigt Zeitstempel in lokaler Zeit', async ({ page, loginAs }) => {
    await loginAs('alarm')
    await page.goto('/alarm/list')
    // Server liefert UTC, die Testumgebung läuft in Europe/Berlin (Sommerzeit).
    await expect(row(page, 'AABBCC')).toContainText('07.05.2026 22:38:45')
  })

  test('fragt absteigend sortiert und seitenweise ab', async ({ page, api, loginAs }) => {
    api.state.alarms = Array.from({ length: 45 }, (_, i) => ({ id: `a${i}`, timestamp: '2026-05-07T20:38:45', rfidId: `KEY${i}`, status: 1 }))
    await loginAs('alarm')
    await page.goto('/alarm/list')
    await expect(row(page, 'KEY0').first()).toBeVisible()
    expect(api.sent('GET', '/alarm/page')[0]!.query.get('sort')).toBe('timestamp,desc')

    await page.getByRole('button', { name: 'Seite 2', exact: true }).click()
    await expect(row(page, 'KEY20').first()).toBeVisible()
    expect(api.sent('GET', '/alarm/page').at(-1)!.query.get('page')).toBe('1')
  })
})
