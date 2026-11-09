import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import NavMenu from '~/components/nav-menu.vue'

const login = (credentials: { username: string, password: string }) => useUserStore().login(credentials)

it ('Does not show menu or credentials without authentication', async () => {
  // The login link is hidden on /login itself, so mount on another route.
  const navMenu = await mountSuspended(NavMenu, { route: '/register' })

  // things that should not be visible
  expect(navMenu.html()).not.toContain('Home')
  expect(navMenu.html()).not.toContain('Participants')
  expect(navMenu.html()).toContain('Log in')

  // things that should be visible
  expect(navMenu.html()).toContain('Choose language')
})

it ('Shows menu and credentials with authentication', async () => {
  await login({username: "testscheduleadmin", password: "howwouldiknow"})
  const navMenu = await mountSuspended(NavMenu)

  // things that should be visible
  expect(navMenu.html()).toContain('Home')
  expect(navMenu.html()).toContain('Service Plans')
  expect(navMenu.html()).not.toContain('Log in')
  expect(navMenu.html()).toContain('testscheduleadmin')
})

it ('Shows alarm menu button for alarm admin', async () => {
  await login({username: "testalarmadmin", password: "howwouldiknow"})
  const navMenu = await mountSuspended(NavMenu)

  // The parent trigger button is in the HTML; dropdown children are teleported
  expect(navMenu.html()).toContain('Alarm system')
  // The alarm section should not appear for schedule admin or regular user
  expect(navMenu.html()).not.toContain('Participants')
})

it ('Does not show alarm menu for non-alarm-admin user', async () => {
  await login({username: "testuser", password: "howwouldiknow"})
  const navMenu = await mountSuspended(NavMenu)

  expect(navMenu.html()).not.toContain('Alarm system')
  expect(navMenu.html()).not.toContain('Event log')
  expect(navMenu.html()).not.toContain('Device Status Log')
})

it ('Groups the schedule pages in one menu entry instead of separate ones', async () => {
  await login({username: "testadmin", password: "howwouldiknow"})
  const navMenu = await mountSuspended(NavMenu)

  expect(navMenu.html()).toContain('Service Plans')
  // The former top level entries are children now, teleported until the menu opens
  expect(navMenu.html()).not.toContain('Participants')
  expect(navMenu.html()).not.toContain('>Services<')
})

it ('Shows the user administration to administrators only', async () => {
  await login({username: "testadmin", password: "howwouldiknow"})
  expect((await mountSuspended(NavMenu)).html()).toContain('Users')

  await login({username: "testscheduleadmin", password: "howwouldiknow"})
  expect((await mountSuspended(NavMenu)).html()).not.toContain('Users')
})

it ('Shows the alarm menu to users holding the right', async () => {
  await login({username: "testdelegate", password: "howwouldiknow"})
  expect((await mountSuspended(NavMenu)).html()).toContain('Alarm system')
})

it ('Offers to leave an impersonation only while impersonating', async () => {
  await login({username: "testadmin", password: "howwouldiknow"})
  expect((await mountSuspended(NavMenu)).html()).not.toContain('Stop impersonating')

  await useUserStore().impersonate('u1')
  const navMenu = await mountSuspended(NavMenu)
  expect(navMenu.html()).toContain('Stop impersonating')
  expect(navMenu.html()).toContain('impersonated')

  await navMenu.find('button[aria-label="Stop impersonating"]').trigger('click')
  await vi.waitFor(() => expect(useUserStore().user?.username).toBe('testadmin'))
  expect(useUserStore().isImpersonating).toBe(false)
})

it ('Shows bug list and settings as separate entries next to the service plans', async () => {
  await login({username: "testadmin", password: "howwouldiknow"})
  const adminMenu = (await mountSuspended(NavMenu)).html()
  // Children are teleported until their menu opens, so visible labels are top level entries
  expect(adminMenu).toContain('Service Plans')
  expect(adminMenu).toContain('/request/list')
  expect(adminMenu).toContain('/settings')

  await login({username: "testuser", password: "howwouldiknow"})
  const userMenu = (await mountSuspended(NavMenu)).html()
  expect(userMenu).toContain('/request/list')
  expect(userMenu).not.toContain('/settings')
})

it ('Hides the service plans entry without any schedule page', async () => {
  await login({username: "testalarmadmin", password: "howwouldiknow"})
  expect((await mountSuspended(NavMenu)).html()).not.toContain('Service Plans')
})
