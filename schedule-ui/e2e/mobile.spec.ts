import type { Role } from './support/mock-api'
import { expect, isMobile, test } from './support/test'

const pages: [Role, string][] = [
  ['user', '/'],
  ['user', '/plan/list'],
  ['user', '/plan/availabilities/me/plan-open'],
  ['user', '/plan/generated/plan-published'],
  ['user', '/request/list'],
  ['admin', '/plan/list'],
  ['admin', '/plan/add'],
  ['admin', '/plan/details/plan-open'],
  ['admin', '/plan/availabilities/plan-open'],
  ['admin', '/plan/statistics/plan-published'],
  ['admin', '/participants/list'],
  ['admin', '/participants/add'],
  ['admin', '/participants/import'],
  ['admin', '/service/list'],
  ['admin', '/service/add'],
  ['admin', '/settings'],
  ['admin', '/alarm/list'],
  ['user', '/permission-denied'],
]

test.describe('Darstellung', () => {
  for (const [role, path] of pages) {
    test(`${path} läuft für ${role} nicht seitlich über`, async ({ page, loginAs }) => {
      await loginAs(role)
      await page.goto(path)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
      await page.waitForLoadState('networkidle')

      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
      expect(overflow).toBeLessThanOrEqual(0)
    })
  }

  test('Anmelde- und Registrierungsseite laufen nicht seitlich über', async ({ page }) => {
    for (const path of ['/login', '/register']) {
      await page.goto(path)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
      expect(overflow).toBeLessThanOrEqual(0)
    }
  })

  test('das Admin-Menü passt auch auf die kleinste Desktop-Breite', async ({ page, loginAs }) => {
    await page.setViewportSize({ width: 1024, height: 768 })
    await loginAs('admin')
    await page.goto('/')
    await expect(page.getByRole('banner').getByText('Alarmanlage', { exact: true })).toBeVisible()
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
    expect(overflow).toBeLessThanOrEqual(0)
  })

  test('die Aktionen der Teilnehmerliste bleiben ohne Scrollen sichtbar', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')
    const box = await page.locator('tbody tr', { hasText: 'Albers' }).getByRole('button', { name: 'Bearbeiten' }).boundingBox()
    expect(box!.x + box!.width).toBeLessThanOrEqual(page.viewportSize()!.width)
  })

  test('bleibt auch bei dunkler Systemeinstellung hell', async ({ page, loginAs }) => {
    await page.emulateMedia({ colorScheme: 'dark' })
    await loginAs('user')
    await page.goto('/plan/list')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

    const { darkClass, background, scheme } = await page.evaluate(() => ({
      darkClass: document.documentElement.classList.contains('dark'),
      background: getComputedStyle(document.body).backgroundColor,
      scheme: getComputedStyle(document.documentElement).colorScheme,
    }))
    expect(darkClass).toBe(false)
    expect(scheme).toBe('light')
    const [r, g, b] = background.match(/[\d.]+/g)!.map(Number)
    // oklch()-Werte beginnen mit der Helligkeit, rgb()-Werte mit 0-255.
    expect(background.startsWith('oklch') ? r! > 0.9 : r! > 230 && g! > 230 && b! > 230).toBe(true)
  })
})

test.describe('Mobile Bedienung', () => {
  test.beforeEach(({ page }) => {
    test.skip(!isMobile(page), 'nur auf kleinen Bildschirmen')
  })

  test('das Menü lässt sich öffnen und schließt nach der Navigation', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await page.getByRole('button', { name: 'Menü öffnen' }).click()

    const dialog = page.getByRole('dialog')
    await dialog.getByRole('link', { name: 'Dienstpläne' }).click()

    await expect(page).toHaveURL(/\/plan\/list$/)
    await expect(dialog).toHaveCount(0)
  })

  test('Tabellenzeilen werden zu beschrifteten Karten', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')
    const row = page.locator('tbody tr', { hasText: 'Albers' })
    await expect(row).toBeVisible()

    const { display, label } = await row.evaluate((element) => {
      const labelled = element.querySelector('td.cell-labelled')!
      return {
        display: getComputedStyle(element).display,
        label: getComputedStyle(labelled, '::before').content,
      }
    })
    expect(display).toBe('block')
    expect(label).toContain('Nachname')
  })

  test('Sortierung bleibt ohne Kopfzeile erreichbar', async ({ page, api, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/participants/list')
    await page.getByRole('button', { name: 'Vorname', exact: true }).click()
    await expect.poll(() => api.sent('GET', '/participant/page').at(-1)!.query.get('sort')).toBe('firstname,asc')
  })

  test('Bedienelemente sind groß genug für Touch', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/plan/availabilities/me/plan-open')
    await expect(page.getByTestId('availability').first()).toBeVisible()

    const buttons = page.getByRole('main').getByRole('button')
    for (const button of await buttons.all()) {
      const box = await button.boundingBox()
      expect(box!.height).toBeGreaterThanOrEqual(44)
    }
  })

  test('die Verfügbarkeitsmatrix scrollt mit fixierter Datumsspalte', async ({ page, loginAs }) => {
    await loginAs('schedule')
    await page.goto('/plan/availabilities/plan-open')
    const dateCell = page.locator('tbody tr').first().locator('td').first()
    await expect(dateCell).toBeVisible()
    expect(await dateCell.evaluate(element => getComputedStyle(element).position)).toBe('sticky')
  })
})
