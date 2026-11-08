import { test as base, expect, type Page } from '@playwright/test'
import { MockApi, sessionFor, type Role } from './mock-api'

type Fixtures = {
  api: MockApi
  /** Starts the page with a stored session for the given role. */
  loginAs: (role: Role) => Promise<void>
}

export const test = base.extend<Fixtures>({
  api: [async ({ page }, use) => {
    const api = new MockApi()
    await api.install(page)
    await use(api)
  }, { auto: true }],

  loginAs: async ({ page }, use) => {
    await use(async (role) => {
      // Seed only once per tab so a logout survives later navigations.
      await page.addInitScript((session) => {
        if (!sessionStorage.getItem('e2e-seeded')) {
          sessionStorage.setItem('e2e-seeded', '1')
          localStorage.setItem('user', JSON.stringify(session.user))
          localStorage.setItem('session', JSON.stringify(session.session))
        }
      }, sessionFor(role))
    })
  },
})

export { expect }

/** Fields keep their accessible name across layouts, whether it comes from a label or a placeholder. */
export const field = (page: Page, name: string) => page.getByRole('textbox', { name, exact: true })

/** A toast's visible title. Toasts also render a hidden copy for screen readers, which must not match. */
export const toast = (page: Page, text: string) => page.locator('[data-slot="title"]', { hasText: text })

export const isMobile = (page: Page) => (page.viewportSize()?.width ?? 1280) < 1024

/** Opens the slide-over navigation on small screens, the desktop header shows it inline. */
export const openNavigation = async (page: Page) => {
  if (isMobile(page)) {
    await page.getByRole('button', { name: 'Menü öffnen' }).click()
  }
}

export const openAccountMenu = (page: Page) => page.getByRole('button', { name: 'Benutzerkonto' }).click()

/** Waits until a navigation triggered by the client-side router has settled on the given path. */
export const expectPath = (page: Page, path: string) => expect(page).toHaveURL(new RegExp(`${path.replace(/[/\[\]]/g, '\\$&')}$`))
