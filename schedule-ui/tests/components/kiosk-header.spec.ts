import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, expect, it, vi } from 'vitest'
import KioskHeader from '~/components/kiosk-header.vue'

const { mockNavigateTo } = vi.hoisted(() => ({ mockNavigateTo: vi.fn() }))
mockNuxtImport('navigateTo', () => mockNavigateTo)

beforeEach(async () => {
  mockNavigateTo.mockClear()
  await useUserStore().loginKiosk({ username: 'anna@example.org', pin: '123456' })
})

it ('Offers only maintenance and operations, no bug list, notifications or personal settings', async () => {
  const header = await mountSuspended(KioskHeader, { route: '/maintenance/defects' })
  const links = header.findAll('nav a').map(link => link.attributes('href'))
  expect(links).toEqual(['/operations', '/maintenance', '/maintenance/defects', '/maintenance/schedule', '/maintenance/inventory', '/maintenance/winch'])
  expect(header.html()).not.toContain('/request/list')
  expect(header.html()).not.toContain('/profile')
  expect(header.html()).not.toContain('/maintenance/templates')
  expect(header.html()).not.toContain('/operations/archive')
  expect(header.find('[data-notification-bell]').exists()).toBe(false)
  expect(header.find('[data-kiosk-user]').text()).toBe('anna@example.org')
  expect(header.text()).toContain('Kiosk')
  expect(header.find('nav a[aria-current="page"]').attributes('href')).toBe('/maintenance/defects')
})

it ('Logs out with the button', async () => {
  const header = await mountSuspended(KioskHeader, { route: '/maintenance' })
  await header.find('[data-kiosk-logout]').trigger('click')
  await vi.waitFor(() => expect(mockNavigateTo).toHaveBeenCalledWith('/login'))
  expect(useUserStore().isLoggedIn).toBe(false)
})

