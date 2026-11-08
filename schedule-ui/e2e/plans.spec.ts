import type { Page } from '@playwright/test'
import { expect, expectPath, field, test } from './support/test'

const row = (page: Page, text: string) => page.locator('tr', { hasText: text })
const card = (page: Page, text: string) => page.locator('article', { hasText: text })
const action = (page: Page, plan: string, name: string) => card(page, plan).getByRole('button', { name, exact: true })

const DETAILS = 'Details'
const AVAILABILITY = 'Verfügbarkeiten'
const MY_AVAILABILITY = 'Eintragen'
const GENERATE = 'Generieren'
const FINAL = 'Dienstplan'
const STATS = 'Statistik'
const PDF = 'PDF'
const CSV = 'CSV'
const DELETE = 'Löschen'

test.describe('Dienstplanliste', () => {
  test('zeigt alle Pläne mit formatiertem Zeitraum', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/plan/list')
    await expect(card(page, 'Winterplan')).toContainText('Dezember 2026')
    await expect(card(page, 'Winterplan')).toContainText('01.12.2026')
    await expect(card(page, 'Winterplan')).toContainText('31.12.2026')
    await expect(card(page, 'Winterplan')).toContainText('Offen')
    await expect(card(page, 'Herbstplan')).toContainText('Veröffentlicht')
    await expect(card(page, 'Neujahrsplan')).toContainText('In Vorbereitung')
  })

  test('sperrt Admin-Aktionen passend zum Planstatus', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/plan/list')

    // offen, mit Dienstterminen
    await expect(action(page, 'Winterplan', DETAILS)).toBeEnabled()
    await expect(action(page, 'Winterplan', AVAILABILITY)).toBeEnabled()
    await expect(action(page, 'Winterplan', GENERATE)).toBeEnabled()
    await expect(action(page, 'Winterplan', FINAL)).toBeDisabled()
    await expect(action(page, 'Winterplan', PDF)).toBeDisabled()

    // veröffentlicht
    await expect(action(page, 'Herbstplan', DETAILS)).toBeDisabled()
    await expect(action(page, 'Herbstplan', FINAL)).toBeEnabled()
    await expect(action(page, 'Herbstplan', STATS)).toBeEnabled()
    await expect(action(page, 'Herbstplan', PDF)).toBeEnabled()
    await expect(action(page, 'Herbstplan', CSV)).toBeEnabled()

    // ohne Diensttermine
    await expect(action(page, 'Neujahrsplan', DETAILS)).toBeEnabled()
    await expect(action(page, 'Neujahrsplan', AVAILABILITY)).toBeDisabled()
    await expect(action(page, 'Neujahrsplan', GENERATE)).toBeDisabled()
  })

  test('zeigt Benutzern nur ihre Aktionen', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/list')

    await expect(action(page, 'Winterplan', MY_AVAILABILITY)).toBeEnabled()
    await expect(action(page, 'Herbstplan', MY_AVAILABILITY)).toBeDisabled()
    await expect(action(page, 'Herbstplan', FINAL)).toBeEnabled()
    await expect(action(page, 'Herbstplan', PDF)).toBeEnabled()

    for (const hidden of [DETAILS, GENERATE, STATS, CSV, DELETE]) {
      await expect(page.getByRole('button', { name: hidden, exact: true })).toHaveCount(0)
    }
  })

  test('führt Benutzer zu ihren Verfügbarkeiten', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/list')
    await action(page, 'Winterplan', MY_AVAILABILITY).click()
    await expectPath(page, '/plan/availabilities/me/plan-open')
  })

  test('generiert einen Plan final', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/plan/list')
    await action(page, 'Winterplan', GENERATE).click()

    await expect(page.getByText('Plan generated successfully.')).toBeVisible()
    expect(api.sent('POST', '/plan/generate/plan-open')).toHaveLength(1)
    await expect(action(page, 'Winterplan', FINAL)).toBeEnabled()
  })

  test('löscht einen Plan erst nach Bestätigung', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/plan/list')

    await action(page, 'Winterplan', DELETE).click()
    await expect(page.getByText('Dienstplan wirklich löschen?')).toBeVisible()
    await page.getByRole('button', { name: 'Abbrechen' }).click()
    expect(api.sent('DELETE', '/plan/delete/plan-open')).toHaveLength(0)
    await expect(card(page, 'Winterplan')).toBeVisible()

    await action(page, 'Winterplan', DELETE).click()
    await page.getByRole('button', { name: 'Bestätigen' }).click()
    await expect(card(page, 'Winterplan')).toHaveCount(0)
    expect(api.sent('DELETE', '/plan/delete/plan-open')).toHaveLength(1)
  })

  test('zeigt Serverfehler an', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    api.fail('POST', /^\/plan\/generate\//, 500)
    await page.goto('/plan/list')
    await action(page, 'Winterplan', GENERATE).click()
    await expect(page.getByRole('alert').or(page.getByText(/500/)).first()).toBeVisible()
  })

  test('lädt das PDF herunter', async ({ page, api, context, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/list')
    const popup = context.waitForEvent('page')
    await action(page, 'Herbstplan', PDF).click()
    await popup
    expect(api.sent('GET', '/plan/download/pdf/plan-published')).toHaveLength(1)
  })

  test('lädt je Dienst eine CSV-Datei herunter', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/plan/list')

    const names: string[] = []
    page.on('download', d => names.push(d.suggestedFilename().normalize('NFC')))
    await action(page, 'Herbstplan', CSV).click()

    await expect.poll(() => names.sort()).toEqual(['Frühdienst.csv', 'Spätdienst.csv'].map(name => name.normalize('NFC')))
  })
})

test.describe('Dienstplan anlegen', () => {
  const pickDay = async (page: Page, label: string, index: number) => {
    await page.getByRole('main').getByRole('button', { name: label, exact: true }).click()
    await page.locator('[data-reka-calendar-cell-trigger]:not([data-outside-view])').nth(index).click()
    await page.keyboard.press('Escape')
  }

  test('legt einen Plan an und öffnet dessen Details', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/add')

    await field(page, 'Dienstplan Name').fill('Frühlingsplan')
    await field(page, 'Beschreibung').fill('März')
    await pickDay(page, 'Von', 0)
    await pickDay(page, 'Bis', 14)
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expectPath(page, '/plan/details/plan-new')
    const body = api.sent('POST', '/plan/save')[0]!.body
    expect(body).toMatchObject({ name: 'Frühlingsplan', description: 'März', published: false })
    expect(new Date(body.dateFrom).getTime()).toBeLessThan(new Date(body.dateUntil).getTime())
  })

  test('meldet Fehler beim Speichern', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    api.fail('POST', /^\/plan\/save$/)
    await page.goto('/plan/add')
    await field(page, 'Dienstplan Name').fill('Kaputt')
    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Fehler beim Speichern des Dienstplans')).toBeVisible()
    await expectPath(page, '/plan/add')
  })
})

test.describe('Dienstplan-Details', () => {
  test('lädt vorhandene Termine und Dienste', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/details/plan-open')

    await expect(page.getByText('Frühdienst').first()).toBeVisible()
    await expect(page.getByText('Spätdienst').first()).toBeVisible()
    await expect(page.getByText(/05\.12\.2026/)).toBeVisible()
    await expect(page.getByText(/06\.12\.2026/)).toBeVisible()
  })

  test('schlägt für neue Pläne Wochenenden und Feiertage vor', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/details/plan-empty')

    for (const date of ['02.01.2027', '03.01.2027', '06.01.2027', '09.01.2027', '10.01.2027']) {
      await expect(page.getByText(new RegExp(date.replace(/\./g, '\\.')))).toBeVisible()
    }
    await expect(page.getByText(/04\.01\.2027/)).toHaveCount(0)
  })

  test('speichert nur Dienste mit Teilnehmerzahl', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/details/plan-open')
    await expect(page.getByText(/05\.12\.2026/)).toBeVisible()

    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Dienstplan erfolgreich gespeichert')).toBeVisible()

    const body = api.sent('POST', '/plan/serviceDates/save/plan-open')[0]!.body
    expect(body.map((e: any) => [e.date.slice(0, 10), e.service.id])).toEqual([
      ['2026-12-05', 's1'], ['2026-12-05', 's2'], ['2026-12-06', 's1'],
    ])
  })

  test('entfernt einen Termin', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/details/plan-open')
    await expect(page.getByText(/05\.12\.2026/)).toBeVisible()

    await page.getByRole('button', { name: 'Löschen' }).first().click()
    await expect(page.getByText(/05\.12\.2026/)).toHaveCount(0)

    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Dienstplan erfolgreich gespeichert')).toBeVisible()
    const body = api.sent('POST', '/plan/serviceDates/save/plan-open')[0]!.body
    expect(body).toHaveLength(1)
    expect(body[0].date).toContain('2026-12-06')
  })

  test('fügt einen Termin über den Kalender hinzu', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/details/plan-open')
    await expect(page.getByText(/05\.12\.2026/)).toBeVisible()
    const before = await page.getByRole('button', { name: 'Löschen' }).count()

    await page.getByRole('button', { name: 'Datum hinzufügen' }).click()
    await page.locator('[data-reka-calendar-cell-trigger]:not([data-outside-view])').nth(20).click()
    await page.keyboard.press('Escape')

    await expect(page.getByRole('button', { name: 'Löschen' })).toHaveCount(before + 1)
  })
})

test.describe('Finaler Dienstplan', () => {
  test('zeigt die Einteilung je Datum und Dienst', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/generated/plan-published')

    await expect(row(page, '03.10.2026')).toContainText('Albers, Anna')
    await expect(row(page, '03.10.2026')).toContainText('Becker, Ben')
    await expect(row(page, '04.10.2026')).toContainText('Becker, Ben')
  })

  test('erlaubt Benutzern keine Bearbeitung', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/generated/plan-published')
    await expect(row(page, '03.10.2026')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Bearbeiten' })).toHaveCount(0)
  })

  test('lässt Administratoren einen Eintrag ändern', async ({ page, api, loginAs }) => {
    api.state.availabilities = [{ date: api.state.plans[1].entries[0].date, participant: api.state.participants[0], requestedService: null }]
    await loginAs('admin')
    await page.goto('/plan/generated/plan-published')

    await row(page, '03.10.2026').getByRole('button', { name: 'Bearbeiten' }).first().click()
    await row(page, '03.10.2026').getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('Dienstplan erfolgreich gespeichert')).toBeVisible()
    const body = api.sent('POST', '/plan/updatePlanEntry/plan-published')[0]!.body
    expect(body).toHaveLength(1)
    expect(body[0]).toMatchObject({ participant: { id: 'p1' }, service: { id: 's1' } })
  })

  test('führt zurück zur Übersicht', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/generated/plan-published')
    await page.getByRole('link', { name: 'Zurück zur Übersicht' }).click()
    await expectPath(page, '/plan/list')
  })
})

test.describe('Statistik', () => {
  test('zeigt Dienste je Teilnehmer und sortiert', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/statistics/plan-published')

    await expect(row(page, 'Anna Albers')).toContainText('4')
    await expect(row(page, 'Ben Becker')).toContainText('2')

    await page.getByRole('button', { name: 'Gesamtanzahl Dienste' }).click()
    await expect(page.locator('tbody tr').first()).toContainText('Ben Becker')
  })
})
