import type { Page } from '@playwright/test'
import { expect, field, test, toast } from './support/test'

const row = (page: Page, text: string) => page.locator('tr', { hasText: text })

test.describe('Teilnehmerliste', () => {
  test('zeigt Teilnehmer mit Status und Diensten', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')

    await expect(row(page, 'Albers')).toContainText('anna@example.org')
    await expect(row(page, 'Albers')).toContainText('15.03.1990')
    await expect(row(page, 'Albers')).toContainText('Aktiver Teilnehmer')
    await expect(row(page, 'Albers')).toContainText('Frühdienst, Spätdienst')
    await expect(row(page, 'Becker')).toContainText('Passiver Teilnehmer')
  })

  test('filtert serverseitig', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')
    await expect(row(page, 'Albers')).toBeVisible()

    await page.getByPlaceholder('Filter').fill('becker')
    await expect(row(page, 'Albers')).toHaveCount(0)
    await expect(row(page, 'Becker')).toBeVisible()
    expect(api.sent('GET', '/participant/page').at(-1)!.query.get('search')).toBe('becker')
  })

  test('sortiert serverseitig', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')
    await expect(row(page, 'Albers')).toBeVisible()

    await page.getByRole('button', { name: 'Vorname', exact: true }).click()
    await expect.poll(() => api.sent('GET', '/participant/page').at(-1)!.query.get('sort')).toBe('firstname,asc')
  })

  test('bearbeitet einen Teilnehmer', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')

    await row(page, 'Albers').getByRole('button', { name: 'Bearbeiten' }).click()
    const editing = page.locator('tr').filter({ has: page.getByRole('button', { name: 'Speichern' }) })
    await editing.locator('input').first().fill('Ahrens')
    await editing.getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('Teilnehmer erfolgreich aktualisiert')).toBeVisible()
    await expect(row(page, 'Ahrens')).toBeVisible()
    expect(api.sent('POST', '/participant/save')[0]!.body).toMatchObject({ id: 'p1', lastname: 'Ahrens' })
  })

  test('bricht die Bearbeitung ohne Speichern ab', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')

    await row(page, 'Albers').getByRole('button', { name: 'Bearbeiten' }).click()
    await page.getByRole('button', { name: 'Abbrechen' }).click()

    await expect(page.getByRole('button', { name: 'Speichern' })).toHaveCount(0)
    expect(api.sent('POST', '/participant/save')).toHaveLength(0)
  })

  test('löscht einen Teilnehmer', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')

    await row(page, 'Becker').getByRole('button', { name: 'Löschen' }).click()
    await expect(page.getByText('Teilnehmer erfolgreich gelöscht')).toBeVisible()
    await expect(row(page, 'Becker')).toHaveCount(0)
    expect(api.sent('DELETE', '/participant/delete/p2')).toHaveLength(1)
  })

  test('meldet Fehler beim Löschen', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    api.fail('DELETE', /^\/participant\/delete\//)
    await page.goto('/participants/list')
    await row(page, 'Becker').getByRole('button', { name: 'Löschen' }).click()
    await expect(page.getByText('Fehler beim Löschen des Teilnehmers')).toBeVisible()
  })

  test('kopiert den Registrierungsschlüssel', async ({ page, loginAs }) => {
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: { writeText: (text: string) => { (window as any).copied = text; return Promise.resolve() } },
      })
    })
    await loginAs('schedule')
    await page.goto('/participants/list')

    await row(page, 'Albers').getByRole('button', { name: 'Kopieren' }).click()
    await expect(toast(page, 'Der Registrierungsschlüssel wurde in die Zwischenablage kopiert.')).toBeVisible()
    expect(await page.evaluate(() => (window as any).copied)).toBe('KEY-p1')
  })

  test('blendet den Schlüssel bei aktivem 2FA-Feature aus', async ({ page, api, loginAs }) => {
    api.state.features = [{ id: 'vfLoginFeature', description: '', active: true }]
    await loginAs('schedule')
    await page.goto('/participants/list')
    await expect(row(page, 'Albers')).toBeVisible()
    await expect(page.getByText('KEY-p1')).toHaveCount(0)
  })

  test('blättert durch die Seiten', async ({ page, api, loginAs }) => {
    const template = api.state.participants[0]!
    for (let i = 0; i < 25; i++) {
      api.state.participants.push({ ...template, id: `x${i}`, lastname: `Zimmer${String(i).padStart(2, '0')}`, email: `z${i}@example.org` })
    }
    await loginAs('schedule')
    await page.goto('/participants/list')
    await expect(row(page, 'Albers')).toBeVisible()

    await page.getByRole('button', { name: 'Seite 2', exact: true }).click()
    await expect.poll(() => api.sent('GET', '/participant/page').at(-1)!.query.get('page')).toBe('1')
    await expect(row(page, 'Zimmer24')).toBeVisible()
  })
})

test.describe('Teilnehmer anlegen', () => {
  const fillNames = async (page: Page) => {
    await field(page, 'Vorname').fill('Clara')
    await field(page, 'Nachname').fill('Conrad')
    await field(page, 'Email').fill('clara@example.org')
  }

  test('speichert mit Status und Diensten', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/add')
    await expect(page.getByText('Frühdienst')).toBeVisible()

    await fillNames(page)
    await page.getByRole('checkbox', { name: 'Frühdienst' }).click()
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('Teilnehmer erfolgreich gespeichert')).toBeVisible()
    const body = api.sent('POST', '/participant/save')[0]!.body
    expect(body).toMatchObject({ firstname: 'Clara', lastname: 'Conrad', email: 'clara@example.org', status: 'ACTIVE' })
    expect(body.participantServices.map((ps: any) => ps.service.id)).toEqual(['s1'])
    await expect(field(page, 'Vorname')).toHaveValue('')
  })

  test('meldet bereits vorhandene Teilnehmer', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    api.fail('POST', /^\/participant\/save$/, 409)
    await page.goto('/participants/add')
    await fillNames(page)
    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Teilnehmer existiert bereits')).toBeVisible()
  })

  test('meldet sonstige Fehler', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    api.fail('POST', /^\/participant\/save$/, 500)
    await page.goto('/participants/add')
    await fillNames(page)
    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Fehler beim Speichern des Teilnehmers')).toBeVisible()
  })
})

test.describe('Teilnehmer importieren', () => {
  const upload = async (page: Page) => {
    await page.locator('input[type="file"]').setInputFiles({
      name: 'teilnehmer.csv', mimeType: 'text/csv', buffer: Buffer.from('a;b'),
    })
    await page.getByRole('button', { name: 'Teilnehmer importieren' }).click()
  }

  test('verlangt eine Datei', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/import')
    await page.getByRole('button', { name: 'Teilnehmer importieren' }).click()
    await expect(page.getByText('Please select a file to upload')).toBeVisible()
  })

  test('stellt die Unterschiede dar', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/import')
    await upload(page)

    await expect(row(page, 'Neu')).toContainText('Neu im Import')
    await expect(row(page, 'Albers')).toContainText('Änderungen')
    await expect(row(page, 'Albers')).toContainText('Annika')
    await expect(row(page, 'Becker')).toContainText('Fehlend im Import')
    await expect(row(page, 'User')).toContainText('Keine Änderungen')
  })

  test('übernimmt neue, geänderte und fehlende Teilnehmer einzeln', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/import')
    await upload(page)

    await row(page, 'Albers').getByRole('button', { name: 'Änderung übernehmen' }).click()
    await expect(toast(page, 'Teilnehmer erfolgreich aktualisiert')).toBeVisible()
    expect(api.sent('POST', '/participant/save')[0]!.body).toMatchObject({ id: 'p1', firstname: 'Annika' })

    await row(page, 'Neu').getByRole('button', { name: 'Teilnehmer hinzufügen' }).click()
    await expect(toast(page, 'Teilnehmer erfolgreich importiert')).toBeVisible()

    await row(page, 'Becker').getByRole('button', { name: 'Teilnehmer entfernen' }).click()
    await expect(toast(page, 'Teilnehmer erfolgreich gelöscht')).toBeVisible()
    expect(api.sent('DELETE', '/participant/delete/p2')).toHaveLength(1)
  })

  test('meldet Fehler beim Hochladen', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    api.fail('POST', /^\/participant\/import$/)
    await page.goto('/participants/import')
    await upload(page)
    await expect(page.getByText('An error occurred while uploading the file')).toBeVisible()
  })
})
