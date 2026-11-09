import type { Page } from '@playwright/test'
import { expect, expectPath, openNavigation, test, toast } from './support/test'

const nav = (page: Page) => page.locator('nav, [role="dialog"]')
const row = (page: Page, text: string) => page.locator('tr', { hasText: text })
const leave = (page: Page) => page.getByRole('button', { name: 'Impersonierung beenden' })

test.describe('Benutzerübersicht', () => {
  test('ist für Administratoren über das Menü erreichbar', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/')
    await openNavigation(page)
    await nav(page).getByRole('link', { name: 'Benutzer', exact: true }).locator('visible=true').first().click()
    await expectPath(page, '/users/list')
  })

  test('listet die Basisdaten der registrierten Benutzer', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/users/list')

    await expect(row(page, 'user@example.org')).toContainText('User, Uta')
    await expect(row(page, 'ben@example.org')).toContainText('Becker, Ben')
    await expect(page.getByRole('columnheader', { name: 'Status' })).toHaveCount(0)
    await expect(row(page, 'admin@example.org')).toContainText('Kein Teilnehmer')
    await expect(row(page, 'admin@example.org')).toContainText('Administrator')
  })

  test('sucht nach Benutzern', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/users/list')
    await expect(row(page, 'ben@example.org')).toBeVisible()

    await page.getByRole('textbox', { name: 'Benutzer suchen' }).fill('Uta')

    await expect(row(page, 'ben@example.org')).toHaveCount(0)
    await expect(row(page, 'user@example.org')).toBeVisible()
    expect(api.sent('GET', '/user/page').at(-1)!.query.get('search')).toBe('Uta')
  })

  test('meldet Fehler beim Laden', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    api.fail('GET', /^\/user\/page$/)
    await page.goto('/users/list')
    await expect(page.getByText('Benutzer konnten nicht geladen werden')).toBeVisible()
  })
})

test.describe('Rechte', () => {
  const right = (page: Page, user: string, name: string) => row(page, user).getByRole('checkbox', { name })

  test('vergibt mehrere Rechte und entzieht sie wieder', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/users/list')

    await expect(right(page, 'user@example.org', 'Alarmanlage verwalten')).toBeChecked()
    await expect(right(page, 'user@example.org', 'Dienstpläne verwalten')).not.toBeChecked()

    await right(page, 'user@example.org', 'Dienstpläne verwalten').click()
    await expect(right(page, 'user@example.org', 'Dienstpläne verwalten')).toBeChecked()
    await expect(right(page, 'user@example.org', 'Alarmanlage verwalten')).toBeChecked()
    expect(api.sent('PUT', '/user/u-user/permissions')[0]!.body).toEqual(['PERMISSION_ALARM_MANAGE', 'PERMISSION_SCHEDULE_MANAGE'])

    await right(page, 'user@example.org', 'Alarmanlage verwalten').click()
    await expect(right(page, 'user@example.org', 'Alarmanlage verwalten')).not.toBeChecked()
    expect(api.sent('PUT', '/user/u-user/permissions')[1]!.body).toEqual(['PERMISSION_SCHEDULE_MANAGE'])
  })

  test('sperrt Rechte, die eine Rolle bereits enthält', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/users/list')

    await expect(right(page, 'admin@example.org', 'Dienstpläne verwalten')).toBeDisabled()
    await expect(right(page, 'admin@example.org', 'Alarmanlage verwalten')).toBeDisabled()
    await expect(right(page, 'schedule@example.org', 'Dienstpläne verwalten')).toBeChecked()
    await expect(right(page, 'schedule@example.org', 'Dienstpläne verwalten')).toBeDisabled()
    await expect(right(page, 'schedule@example.org', 'Alarmanlage verwalten')).toBeEnabled()
  })

  test('behält den Stand, wenn das Speichern fehlschlägt', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    api.fail('PUT', /^\/user\/u-user\/permissions$/)
    await page.goto('/users/list')

    await right(page, 'user@example.org', 'Dienstpläne verwalten').click()
    await expect(toast(page, 'Rechte konnten nicht geändert werden')).toBeVisible()
    await expect(right(page, 'user@example.org', 'Dienstpläne verwalten')).not.toBeChecked()
  })
})

test.describe('Impersonierung', () => {
  const impersonate = (page: Page, user: string) => row(page, user).getByRole('button', { name: 'Impersonieren' })

  test('zeigt die Anwendung aus Sicht des Benutzers und lässt sich oben rechts beenden', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    await page.goto('/users/list')
    await expect(leave(page)).toHaveCount(0)

    await impersonate(page, 'user@example.org').click()

    await expectPath(page, '/')
    await expect(leave(page)).toBeVisible()
    await expect(page.getByRole('main').getByRole('link', { name: 'Benutzer', exact: true })).toHaveCount(0)
    await openNavigation(page)
    await expect(nav(page).getByRole('link', { name: 'Einstellungen', exact: true })).toHaveCount(0)

    // nachfolgende Anfragen laufen mit dem Token des Benutzers
    await page.goto('/users/list')
    await expectPath(page, '/permission-denied')

    await leave(page).click()
    await expectPath(page, '/users/list')
    await expect(leave(page)).toHaveCount(0)
    await expect(row(page, 'user@example.org')).toBeVisible()
    expect(api.sent('POST', '/user/impersonation/stop')).toHaveLength(1)
  })

  test('übersteht ein Neuladen der Seite', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/users/list')
    await impersonate(page, 'user@example.org').click()
    await expect(leave(page)).toBeVisible()

    await page.reload()
    await expect(leave(page)).toBeVisible()
  })

  test('bietet keine Impersonierung von Administratoren an', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/users/list')
    await expect(impersonate(page, 'admin@example.org')).toBeDisabled()
    await expect(impersonate(page, 'schedule@example.org')).toBeEnabled()
  })

  test('meldet Fehler und bleibt angemeldet', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    api.fail('POST', /^\/user\/u-user\/impersonate$/)
    await page.goto('/users/list')

    await impersonate(page, 'user@example.org').click()
    await expect(toast(page, 'Impersonierung fehlgeschlagen')).toBeVisible()
    await expect(leave(page)).toHaveCount(0)
  })

  test('führt zur Anmeldung, wenn das Beenden fehlschlägt', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    api.fail('POST', /^\/user\/impersonation\/stop$/, 403)
    await page.goto('/users/list')
    await impersonate(page, 'user@example.org').click()

    await leave(page).click()
    await expectPath(page, '/login')
    await expect(leave(page)).toHaveCount(0)
  })
})

test.describe('Rechte im Hauptmenü', () => {
  test('zeigt Inhabern des Rechts die Verwaltung der Dienstpläne', async ({ page, loginAs }) => {
    await loginAs('delegate')
    await page.goto('/')
    await expect(page.getByRole('main').getByRole('link', { name: 'Teilnehmer anzeigen' })).toBeVisible()
    await expect(page.getByRole('main').getByRole('link', { name: 'Ereignisprotokoll' })).toHaveCount(0)
  })
})
