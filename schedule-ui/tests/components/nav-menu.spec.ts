import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it } from 'vitest'
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
  expect(navMenu.html()).toContain('Participants')
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