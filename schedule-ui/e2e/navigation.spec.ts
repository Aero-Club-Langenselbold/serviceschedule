import type { Page } from '@playwright/test'
import { expect, expectPath, openAccountMenu, openNavigation, openSchedules, test } from './support/test'
import type { Role } from './support/mock-api'

const nav = (page: Page) => page.locator('nav, [role="dialog"]')

test.describe('Rollen und Menü', () => {
  const visibility: [Role, string[], string[]][] = [
    ['admin', ['Dienstpläne', 'Alarmanlage', 'Bugs & Wünsche', 'Einstellungen', 'Benutzer'], []],
    ['schedule', ['Dienstpläne', 'Bugs & Wünsche'], ['Alarmanlage', 'Einstellungen', 'Benutzer']],
    ['alarm', ['Alarmanlage', 'Bugs & Wünsche'], ['Dienstpläne', 'Einstellungen', 'Benutzer']],
    ['user', ['Dienstpläne', 'Bugs & Wünsche'], ['Alarmanlage', 'Einstellungen', 'Benutzer']],
  ]

  for (const [role, shown, hidden] of visibility) {
    test(`zeigt die passenden Einträge für ${role}`, async ({ page, loginAs }) => {
      await loginAs(role)
      await page.goto('/')
      await openNavigation(page)
      for (const label of shown) {
        await expect(nav(page).getByText(label, { exact: true }).locator('visible=true').first()).toBeVisible()
      }
      for (const label of hidden) {
        await expect(nav(page).getByText(label, { exact: true })).toHaveCount(0)
      }
    })
  }

  const guarded: [Role, string, boolean][] = [
    ['user', '/participants/list', false],
    ['user', '/service/list', false],
    ['user', '/plan/add', false],
    ['user', '/settings', false],
    ['user', '/alarm/list', false],
    ['alarm', '/participants/list', false],
    ['schedule', '/alarm/list', false],
    ['schedule', '/participants/list', true],
    ['alarm', '/alarm/list', true],
    ['admin', '/alarm/list', true],
    ['admin', '/settings', true],
    ['user', '/plan/list', true],
    ['user', '/request/list', true],
    ['user', '/users/list', false],
    ['schedule', '/users/list', false],
    ['delegate', '/users/list', false],
    ['admin', '/users/list', true],
    // Rights open a module without a matching role
    ['delegate', '/participants/list', true],
    ['delegate', '/alarm/list', false],
  ]

  for (const [role, path, allowed] of guarded) {
    test(`${role} ${allowed ? 'darf' : 'darf nicht'} auf ${path}`, async ({ page, loginAs }) => {
      await loginAs(role)
      await page.goto(path)
      if (allowed) {
        await expectPath(page, path)
        await expect(page.getByText('Zugang verweigert')).toHaveCount(0)
      } else {
        await expectPath(page, '/permission-denied')
        await expect(page.getByText('Zugang verweigert')).toBeVisible()
      }
    })
  }
})

test.describe('Benutzermenü', () => {
  test('zeigt den Benutzernamen', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await openAccountMenu(page)
    await expect(page.getByRole('menu').getByText('user@example.org')).toBeVisible()
  })

  test('enthält weder Einstellungen noch Bug-Liste', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/')
    await openAccountMenu(page)
    await expect(page.getByRole('menuitem', { name: 'Abmelden' })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: 'Einstellungen' })).toHaveCount(0)
    await expect(page.getByRole('menuitem', { name: 'Bugs & Wünsche' })).toHaveCount(0)
  })
})

test.describe('Hauptmenü', () => {
  test('führt über das Menü zu den Teilnehmern', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/')
    await openNavigation(page)
    await openSchedules(page)
    await nav(page).getByRole('link', { name: 'Teilnehmer anzeigen' }).locator('visible=true').first().click()
    await expectPath(page, '/participants/list')
  })

  const scheduleEntries: [Role, string[], string[]][] = [
    ['admin', ['Teilnehmer anzeigen', 'Dienste anzeigen', 'Dienstpläne anzeigen', 'Dienstplan hinzufügen'], []],
    ['schedule', ['Teilnehmer anzeigen', 'Dienste anzeigen', 'Dienstpläne anzeigen'], []],
    ['delegate', ['Teilnehmer anzeigen', 'Dienste anzeigen'], []],
    ['user', ['Dienstpläne anzeigen'], ['Teilnehmer anzeigen', 'Dienste anzeigen', 'Dienstplan hinzufügen']],
  ]

  for (const [role, shown, hidden] of scheduleEntries) {
    test(`fasst die Seiten für ${role} unter Dienstpläne zusammen`, async ({ page, loginAs }) => {
      await loginAs(role)
      await page.goto('/')
      await openNavigation(page)
      await openSchedules(page)
      for (const label of shown) {
        await expect(nav(page).getByRole('link', { name: label, exact: true }).locator('visible=true').first()).toBeVisible()
      }
      for (const label of hidden) {
        await expect(nav(page).getByRole('link', { name: label, exact: true }).locator('visible=true')).toHaveCount(0)
      }
    })
  }

  for (const [width, language] of [[1024, 'de'], [1279, 'de'], [1280, 'de'], [1280, 'en'], [1536, 'de']] as const) {
    test(`passt bei ${width} px auf ${language} in die Seitenbreite`, async ({ page, loginAs }) => {
      await loginAs('admin')
      await page.addInitScript(locale => localStorage.setItem('locale', locale), language)
      await page.setViewportSize({ width, height: 800 })
      await page.goto('/')
      await expect(page.getByRole('button', { name: /Benutzerkonto|Account/ })).toBeVisible()

      const { scroll, client } = await page.evaluate(() => ({ scroll: document.documentElement.scrollWidth, client: document.documentElement.clientWidth }))
      expect(scroll).toBeLessThanOrEqual(client)
    })
  }

  test('führt Administratoren direkt zu den Einstellungen', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/')
    await openNavigation(page)
    await nav(page).getByRole('link', { name: 'Einstellungen', exact: true }).locator('visible=true').first().click()
    await expectPath(page, '/settings')
  })

  test('führt Benutzer direkt zur Bug-Liste', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await openNavigation(page)
    await nav(page).getByRole('link', { name: 'Bugs & Wünsche', exact: true }).locator('visible=true').first().click()
    await expectPath(page, '/request/list')
  })

  test('führt über die Kacheln der Startseite zu den Dienstplänen', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await page.getByRole('main').getByRole('link', { name: 'Dienstpläne' }).click()
    await expectPath(page, '/plan/list')
  })
})

test.describe('Sprache', () => {
  test('wechselt die Sprache und merkt sie sich', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await expect(page.getByText('Herzlich willkommen')).toBeVisible()

    await page.getByRole('button', { name: 'Sprache wählen' }).click()
    await page.getByRole('menuitem', { name: 'English' }).click()
    await expect(page.getByText('Welcome to the Member Area of the ACL')).toBeVisible()

    await page.reload()
    await expect(page.getByText('Welcome to the Member Area of the ACL')).toBeVisible()
  })

  test('übernimmt die gespeicherte Sprache', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.addInitScript(() => localStorage.setItem('locale', 'en'))
    await page.goto('/')
    await expect(page.getByText('Welcome to the Member Area of the ACL')).toBeVisible()
  })
})
