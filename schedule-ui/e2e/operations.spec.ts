import type { Page } from '@playwright/test'
import { expect, expectPath, openAccountMenu, test, toast } from './support/test'

const item = (page: Page, position: number) => page.locator(`[data-item="${position}"]`)

test.describe('Checklisten für Betriebsbeginn und -ende', () => {
  test('ein Mitglied beginnt die Checkliste über den festen Link, prüft, meldet einen Mangel und bestätigt', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/operations/checklist/a1/start')
    await expect(page.getByRole('heading', { name: 'Betriebsbeginn' })).toBeVisible()
    await expect(page.locator('[data-start]')).toContainText('Reifendruck prüfen')

    await page.getByRole('button', { name: 'Checkliste beginnen' }).click()
    await expect(page.locator('[data-created-by]')).toContainText('user@example.org')
    expect(api.sent('POST', '/operations/aircraft/a1/checklists/START/today')).toHaveLength(1)

    await item(page, 0).getByRole('button', { name: 'OK', exact: true }).click()
    await expect(item(page, 0)).toContainText('Geprüft von user@example.org')

    await item(page, 1).getByRole('button', { name: 'Mangel melden' }).click()
    await page.getByRole('dialog').getByRole('textbox').first().fill('Ventil undicht')
    await page.getByRole('dialog').getByRole('button', { name: 'Mangel melden' }).click()
    await expect(toast(page, 'Mangel ins Mängelbuch eingetragen')).toBeVisible()
    await expect(item(page, 1)).toContainText('Mangel: Ventil undicht')
    expect(api.state.defects[0]).toMatchObject({ title: 'Ventil undicht', aircraftId: 'a1' })

    await page.getByRole('button', { name: 'Vollständig – bestätigen' }).click()
    await expect(page.locator('[data-checklist-status]')).toHaveText('Freigabe offen')
    await expect(page.getByRole('button', { name: 'OK', exact: true }).first()).toBeDisabled()
  })

  test('pro Tag gibt es nur eine Checkliste: ein zweiter Besuch landet in derselben', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/operations/checklist/a1/start')
    await page.getByRole('button', { name: 'Checkliste beginnen' }).click()
    await expect(page.locator('[data-checklist-header]')).toBeVisible()

    await page.goto('/operations')
    await expect(page.locator('[data-aircraft="a1"] [data-kind="START"]')).toContainText('In Bearbeitung')
    await page.locator('[data-aircraft="a1"] [data-kind="START"] [data-open]').click()
    await expectPath(page, '/operations/checklist/a1/start')
    await expect(page.locator('[data-created-by]')).toContainText('user@example.org')
    expect(api.state.checklists).toHaveLength(1)
  })

  test('Betriebsadmins legen Checklisten an, drucken den QR-Code und geben im Archiv frei', async ({ page, api, loginAs }) => {
    api.state.checklists.push({ id: 'cl-old', aircraftId: 'a1', registration: 'D-1234', aircraftType: 'ASK 21', kind: 'START', date: new Date().toISOString().slice(0, 10),
      status: 'SUBMITTED', createdBy: { id: 'u-ben', name: 'Becker, Ben' }, createdAt: '2026-11-27T07:00:00Z', submittedBy: { id: 'u-ben', name: 'Becker, Ben' },
      submittedAt: '2026-11-27T08:00:00Z', approvedBy: null, approvedAt: null,
      items: [{ id: 'i0', position: 0, text: 'Haube reinigen', result: 'OK', comment: null, checkedBy: { id: 'u-ben', name: 'Becker, Ben' }, checkedAt: null, defect: null }] })
    await loginAs('operations')
    await page.goto('/operations')

    // the winch gets an end of operations checklist
    await page.locator('[data-aircraft="w1"] [data-kind="END"] [data-create-template]').click()
    await page.getByRole('dialog').getByRole('textbox').fill('Seil einziehen\nAbdeckung drauf')
    await page.getByRole('dialog').getByRole('button', { name: 'Speichern' }).click()
    await expect(page.locator('[data-aircraft="w1"] [data-kind="END"]')).toContainText('2 Punkte')
    expect(api.sent('PUT', '/operations/aircraft/w1/templates/END')[0]!.body).toEqual({ items: ['Seil einziehen', 'Abdeckung drauf'] })

    // print view: registration as heading, QR code below
    await page.goto('/operations/print/w1/end')
    await expect(page.locator('[data-print-title]')).toHaveText('Winde 1')
    await expect(page.locator('[data-qr] svg')).toBeVisible()
    await expect(page.locator('nav')).toHaveCount(0)

    await page.goto('/operations/archive')
    await page.locator('[data-open-checklist="cl-old"]').first().click()
    await expectPath(page, '/operations/checklists/cl-old')
    await page.getByRole('button', { name: 'Erledigung bestätigen' }).click()
    await expect(page.locator('[data-checklist-status]')).toHaveText('Freigegeben')
    await expect(page.locator('[data-approved-by]')).toContainText('operations@example.org')
  })

  test('Mitglieder sehen weder Archiv noch Verwaltung der Checklisten', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/operations')
    await expect(page.locator('[data-aircraft="a1"]')).toBeVisible()
    await expect(page.locator('[data-edit-template]')).toHaveCount(0)
    await expect(page.getByRole('link', { name: 'Checklisten-Archiv' })).toHaveCount(0)

    await page.goto('/operations/archive')
    await expectPath(page, '/permission-denied')
  })
})

test.describe('Persönliche Einstellungen', () => {
  test('öffnen sich über den Benutzernamen und speichern den Kiosk-Code, ohne ihn anzuzeigen', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await openAccountMenu(page)
    await page.getByRole('menuitem', { name: /user@example.org/ }).click()
    await expectPath(page, '/profile')
    await expect(page.locator('[data-kiosk-state]')).toHaveText('Nicht gesetzt')

    await page.locator('[data-pin] input[data-slot="base"]').first().click()
    await page.keyboard.type('012345')
    await page.locator('[data-pin-confirm] input[data-slot="base"]').first().click()
    await page.keyboard.type('012345')
    await page.getByRole('button', { name: 'PIN speichern' }).click()

    await expect(page.locator('[data-kiosk-state]')).toHaveText('Gesetzt')
    expect(api.sent('PUT', '/user/me/kiosk-pin')[0]!.body).toEqual({ pin: '012345' })
    for (const input of await page.locator('[data-pin] input[data-slot="base"]').all()) {
      await expect(input).toHaveAttribute('type', 'password')
      await expect(input).toHaveValue('')
    }
  })
})
