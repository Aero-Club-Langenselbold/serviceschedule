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

it ('Shows bug list and settings as icons only in the header bar', async () => {
  await login({username: "testadmin", password: "howwouldiknow"})
  const navMenu = await mountSuspended(NavMenu)
  for (const [path, label] of [['/request/list', 'Bugs & requests'], ['/settings', 'Settings']]) {
    const link = navMenu.find(`a[href="${path}"]`)
    expect(link.attributes('title')).toBe(label)
    expect(link.find('[data-slot="linkLabel"]').classes()).toContain('sr-only')
  }
})

it ('Shows the service plans to a member holding another right, who is a user as well', async () => {
  await login({username: "testalarmadmin", password: "howwouldiknow"})
  const html = (await mountSuspended(NavMenu)).html()
  expect(html).toContain('Service Plans')
  expect(html).not.toContain('/participants/list')
})

it ('Shows the maintenance menu to every signed in user', async () => {
  await login({username: "testuser", password: "howwouldiknow"})
  expect((await mountSuspended(NavMenu)).html()).toContain('Maintenance')

  await login({username: "testalarmadmin", password: "howwouldiknow"})
  expect((await mountSuspended(NavMenu)).html()).toContain('Maintenance')
})

it ('Offers the defect book in the maintenance menu and the notifications to every signed in user', async () => {
  await login({ username: 'testuser', password: 'howwouldiknow' })
  const navMenu = await mountSuspended(NavMenu)

  expect(navMenu.find('[data-notification-bell]').exists()).toBe(true)
  const maintenance = navMenu.findAll('button').find(button => button.text().includes('Maintenance'))
  expect(maintenance).toBeDefined()
})

it ('Shows the club logo instead of the app name and leads home', async () => {
  await login({ username: 'testuser', password: 'howwouldiknow' })
  const navMenu = await mountSuspended(NavMenu)
  const logo = navMenu.find('[data-club-logo]')
  expect(logo.exists()).toBe(true)
  expect(logo.attributes('src')).toContain('logo')
  expect(logo.attributes('alt')).toContain('Aero-Club Langenselbold')
  expect(navMenu.find('a[href="/"] [data-club-logo]').exists()).toBe(true)
})

it ('Shows the operations menu with the checklists to every signed in user', async () => {
  await login({ username: 'testuser', password: 'howwouldiknow' })
  const navMenu = await mountSuspended(NavMenu)
  expect(navMenu.findAll('button').some(button => button.text().includes('Operations'))).toBe(true)
})

it ('Shows only the icon of the user administration in the bar', async () => {
  await login({ username: 'testadmin', password: 'howwouldiknow' })
  const navMenu = await mountSuspended(NavMenu)
  const users = navMenu.find('a[href="/users/list"]')
  expect(users.exists()).toBe(true)
  expect(users.attributes('title')).toBe('Users')
  expect(users.find('.sr-only').text()).toBe('Users')
})

it ('Opens the personal settings from the username', async () => {
  await login({ username: 'testuser', password: 'howwouldiknow' })
  const navMenu = await mountSuspended(NavMenu)
  await navMenu.find('button[aria-label="Account"]').trigger('click')
  await vi.waitFor(() => expect(document.body.querySelector('[data-profile-link]')).not.toBeNull())
  const link = document.body.querySelector('[data-profile-link]')!
  expect(link.getAttribute('href')).toBe('/profile')
  expect(link.textContent).toContain('testuser')
  expect(link.textContent).toContain('My settings')
})
