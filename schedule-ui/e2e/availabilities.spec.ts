import type { Page } from '@playwright/test'
import { expect, test } from './support/test'

const row = (page: Page, text: string) => page.getByTestId('availability').filter({ hasText: text })
const LABEL = { yes: 'Ich kann', no: 'Ich kann nicht', please: 'Möchte ich gerne' } as const
type State = keyof typeof LABEL

const option = (page: Page, date: string, state: State) => row(page, date).getByRole('button', { name: LABEL[state], exact: true })
const setState = (page: Page, date: string, state: State) => option(page, date, state).click()
const expectState = (page: Page, date: string, state: State) => expect(option(page, date, state)).toHaveAttribute('aria-pressed', 'true')

test.describe('Eigene Verfügbarkeiten', () => {
  test('zeigt einmal je Termin den Standardstatus aktiver Teilnehmer', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/availabilities/me/plan-open')

    await expect(page.getByTestId('availability')).toHaveCount(2)
    await expectState(page, '05.12.2026', 'yes')
  })

  test('passive Teilnehmer starten mit "Ich kann nicht"', async ({ page, api, loginAs }) => {
    api.state.participants[2]!.status = 'INACTIVE'
    await loginAs('user')
    await page.goto('/plan/availabilities/me/plan-open')
    await expectState(page, '05.12.2026', 'no')
  })

  test('speichert nur verfügbare Tage', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/availabilities/me/plan-open')
    await expect(page.getByTestId('availability')).toHaveCount(2)

    await setState(page, '05.12.2026', 'no')
    await expect(page.getByText('Noch nicht gespeichert')).toBeVisible()
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('Preferences saved successfully.')).toBeVisible()
    const body = api.sent('POST', '/plan/participantAvailability/save/plan-open')[0]!.body
    expect(body).toHaveLength(1)
    expect(body[0].date).toContain('2026-12-06')
    expect(body[0].participant.id).toBe('p3')
    await expect(page.getByText('Noch nicht gespeichert')).toBeVisible()
  })

  test('nimmt einen Dienstwunsch auf', async ({ page, api, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/availabilities/me/plan-open')
    await expect(page.getByTestId('availability')).toHaveCount(2)

    await setState(page, '05.12.2026', 'please')
    await expect(row(page, '05.12.2026').getByRole('combobox')).toContainText('Frühdienst')
    await expect.poll(() => api.sent('GET', /^\/plan\/availableServices\/plan-open\//).length).toBeGreaterThan(0)

    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Preferences saved successfully.')).toBeVisible()
    const body = api.sent('POST', '/plan/participantAvailability/save/plan-open')[0]!.body
    expect(body.find((e: any) => e.date.includes('2026-12-05') || e.date.includes('2026-12-04')).requestedService.id).toBe('s1')
  })

  test('stellt gespeicherte Angaben wieder her', async ({ page, api, loginAs }) => {
    const [first] = api.state.plans[0].serviceDates
    api.state.availabilities = [{ date: first.date, requestedService: api.state.services[1], participant: api.state.participants[2] }]
    await loginAs('user')
    await page.goto('/plan/availabilities/me/plan-open')

    await expectState(page, '05.12.2026', 'please')
    await expect(row(page, '05.12.2026').getByRole('combobox')).toContainText('Spätdienst')
    await expectState(page, '06.12.2026', 'no')
    await expect(page.getByText('Gespeichert', { exact: true })).toBeVisible()
  })

  test('meldet zu wenige verfügbare Tage', async ({ page, api, loginAs }) => {
    await loginAs('user')
    api.fail('POST', /^\/plan\/participantAvailability\/save\//, 400, { message: 'availabilities.invalid', value: 3 })
    await page.goto('/plan/availabilities/me/plan-open')
    await expect(page.getByTestId('availability')).toHaveCount(2)
    await page.getByRole('button', { name: 'Speichern' }).click()
    await expect(page.getByText('Minimale Anzahl an verfügbaren Tagen: 3')).toBeVisible()
  })
})

test.describe('Verfügbarkeiten aller Teilnehmer', () => {
  test('zeigt Termine und Teilnehmer als Matrix', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/availabilities/plan-open')

    for (const name of ['Albers, Anna', 'Becker, Ben', 'User, Uta']) {
      await expect(page.getByText(name)).toBeVisible()
    }
    await expect(page.locator('tbody tr')).toHaveCount(2)
  })

  test('filtert Teilnehmer nach Dienst', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/availabilities/plan-open')
    await expect(page.getByText('Becker, Ben')).toBeVisible()

    await page.getByRole('checkbox', { name: 'Spätdienst' }).click()
    await expect(page.getByText('Becker, Ben')).toBeVisible()

    await page.getByRole('checkbox', { name: 'Frühdienst' }).click()
    await expect(page.getByText('Becker, Ben')).toHaveCount(0)
    await expect(page.getByText('Albers, Anna')).toHaveCount(0)
  })

  test('"Alle auswählen" blendet alle aus und wieder ein', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/availabilities/plan-open')
    await expect(page.getByText('Becker, Ben')).toBeVisible()

    await page.getByRole('checkbox', { name: 'Alle auswählen' }).click()
    await expect(page.getByText('Becker, Ben')).toHaveCount(0)

    await page.getByRole('checkbox', { name: 'Alle auswählen' }).click()
    await expect(page.getByText('Becker, Ben')).toBeVisible()
  })

  test('speichert geänderte Teilnehmer', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/availabilities/plan-open')

    await page.getByRole('button', { name: /^Albers, Anna – 5\.12\.2026/ }).click()
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('Preferences saved successfully.')).toBeVisible()
    const body = api.sent('POST', '/plan/participantAvailability/save/plan-open')[0]!.body
    // Anna ist am ersten Tag nun nicht verfügbar, nur der zweite Tag bleibt übrig.
    expect(body.map((e: any) => e.participant.id)).toEqual(['p1'])
    expect(body[0].date).toContain('2026-12-06')
  })

  test('erklärt die Symbole in einer Legende', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/availabilities/plan-open')
    await page.getByRole('button', { name: 'Legende' }).click()
    await expect(page.getByText('Bedeutung der Buttons')).toBeVisible()
    await expect(page.getByText('Verfügbar (gespeichert)', { exact: true })).toBeVisible()
  })
})
