import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it } from 'vitest'
import NavMenu from '~/components/nav-menu.vue'

const userStore = useUserStore()
const { login } = userStore

it ('Can render Nav Menu', async () => {
  const navMenu = await mountSuspended(NavMenu)
  expect(navMenu.html()).toMatchSnapshot()
})

it ('Does not show menu or credentials without authentication', async () => {
  const navMenu = await mountSuspended(NavMenu)

  // things that should not be visible
  expect(navMenu.html()).not.toContain('Home')
  expect(navMenu.html()).not.toContain('Manage Participants')
  expect(navMenu.html()).not.toContain('Logout')
  expect(navMenu.html()).toContain('Login')

  // things that should be visible
  expect(navMenu.html()).toContain('i-circle-flags:de')
})

it ('Shows menu and credentials with authentication', async () => {
  await login({username: "testuser", password: "howwouldiknow"})
  const navMenu = await mountSuspended(NavMenu)

  // things that should be visible
  expect(navMenu.html()).toContain('Home')
  expect(navMenu.html()).toContain('Manage Participants')
  expect(navMenu.html()).toContain('Logout')
  expect(navMenu.html()).not.toContain('Login')
  expect(navMenu.html()).toContain('testuser')
})