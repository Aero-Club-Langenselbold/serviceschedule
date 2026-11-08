import { expect, expectPath, field, openAccountMenu, test } from './support/test'

test.describe('Anmeldung', () => {
  const fill = async (page: any, username: string, password: string) => {
    await field(page, 'Benutzername').fill(username)
    await field(page, 'Passwort').fill(password)
    await page.getByRole('button', { name: 'Anmelden' }).click()
  }

  test('meldet Benutzer an und zeigt die Startseite', async ({ page, api }) => {
    await page.goto('/login')
    await fill(page, 'user@example.org', 'geheim')

    await expectPath(page, '/')
    await expect(page.getByText('Uta User')).toBeVisible()
    expect(api.sent('POST', '/auth/signin')[0]!.body).toMatchObject({ username: 'user@example.org', password: 'geheim' })
  })

  test('zeigt eine Fehlermeldung bei falschen Zugangsdaten', async ({ page }) => {
    await page.goto('/login')
    await fill(page, 'user@example.org', 'wrong')

    await expect(page.getByText('Ungültige Anmeldeinformationen')).toBeVisible()
    await expectPath(page, '/login')
  })

  test('schaltet die Passwortanzeige um', async ({ page }) => {
    await page.goto('/login')
    await expect(field(page, 'Passwort')).toHaveAttribute('type', 'password')
    await page.getByRole('button', { name: 'Passwort anzeigen' }).click()
    await expect(field(page, 'Passwort')).toHaveAttribute('type', 'text')
  })

  test('bleibt nach einem Neuladen angemeldet', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await expect(page.getByText('Uta User')).toBeVisible()
    await page.reload()
    await expect(page.getByText('Uta User')).toBeVisible()
    await expectPath(page, '/')
  })

  test('meldet ab und entfernt die Sitzung', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await openAccountMenu(page)
    await page.getByRole('menuitem', { name: 'Abmelden' }).click()

    await expectPath(page, '/login')
    expect(await page.evaluate(() => localStorage.getItem('session'))).toBeNull()
    await page.goto('/plan/list')
    await expectPath(page, '/login')
  })

  test('leitet ohne Sitzung auf die Anmeldung um', async ({ page }) => {
    for (const path of ['/', '/plan/list', '/participants/list', '/service/list', '/alarm/list', '/settings', '/request/list']) {
      await page.goto(path)
      await expectPath(page, '/login')
    }
  })

  test('leitet bei einer abgelaufenen Sitzung (401) auf die Anmeldung um', async ({ page, api, loginAs }) => {
    await loginAs('admin')
    api.fail('GET', /^\/plan\/list$/, 401)
    await page.goto('/plan/list')
    await expectPath(page, '/login')
  })

  test('fragt bei aktivem Feature die Sicherheitsfrage ab', async ({ page, api }) => {
    api.state.features = [{ id: 'securityQuestionFeature', description: '', active: true }]
    await page.goto('/login')

    await expect(page.getByText('Wie heißt dein Haustier?')).toBeVisible()
    await field(page, 'Antwort auf Sicherheitsfrage').fill('Bello')
    await fill(page, 'user@example.org', 'geheim')

    await expectPath(page, '/')
    expect(api.sent('POST', '/auth/signin')[0]!.body.securityAnswer).toBe('Bello')
  })

  test('verlangt bei aktivem 2FA-Feature den Code und blendet die Registrierung aus', async ({ page, api }) => {
    api.state.features = [{ id: 'vfLoginFeature', description: '', active: true }]
    await page.goto('/login')

    await expect(field(page, 'Sicherheitscode (bei aktivierter 2FA)')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Registrieren' })).toHaveCount(0)
    await expect(page.getByText('Passwort vergessen?')).toHaveCount(0)
  })
})

test.describe('Startseite', () => {
  test('begrüßt Teilnehmer mit Namen', async ({ page, loginAs }) => {
    await loginAs('user')
    await page.goto('/')
    await expect(page.getByText('Uta User')).toBeVisible()
    await expect(page.getByText('deine eigenen Verfügbarkeiten eintragen')).toBeVisible()
  })

  test('weist Administratoren auf ihre Rolle hin', async ({ page, loginAs }) => {
    await loginAs('admin')
    await page.goto('/')
    await expect(page.getByText('in der Rolle als Administrator')).toBeVisible()
  })

  test('weist Benutzer ohne zugeordneten Teilnehmer darauf hin', async ({ page, loginAs }) => {
    await loginAs('nobody')
    await page.goto('/')
    await expect(page.getByText('dir ist aber noch kein Teilnehmer zugeordnet')).toBeVisible()
  })
})
