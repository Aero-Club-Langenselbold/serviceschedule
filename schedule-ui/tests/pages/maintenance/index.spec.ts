import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import MaintenanceIndex from '~/pages/maintenance/index.vue'

const login = (username: string) => useUserStore().login({ username, password: 'howwouldiknow' })

it ('Lists the aircraft with their progress for every user', async () => {
  await login('testuser')
  const page = await mountSuspended(MaintenanceIndex)

  await vi.waitFor(() => expect(page.html()).toContain('D-1234'))
  expect(page.html()).toContain('ASK 21')
  expect(page.html()).toContain('1 of 4 done')
  expect(page.html()).toContain('2 open defects')
  expect(page.html()).toContain('3 tasks without workshop period')
  expect(page.html()).toContain('href="/maintenance/a2?visit=none"')
})

it ('Links every running or upcoming workshop visit and finished ones only on request', async () => {
  await login('testuser')
  const page = await mountSuspended(MaintenanceIndex)
  await vi.waitFor(() => expect(page.find('[data-visit="s1"]').exists()).toBe(true))

  expect(page.find('[data-visit="s1"]').attributes('href')).toBe('/maintenance/a1?visit=s1')
  expect(page.find('[data-visit="s1"]').text()).toContain('Running')
  expect(page.find('[data-visit="s2"]').attributes('href')).toBe('/maintenance/a2?visit=s2')
  expect(page.find('[data-visit="s0"]').exists()).toBe(false)

  const toggle = page.find('[data-aircraft="a1"] [data-show-past]')
  expect(toggle.text()).toBe('Show 1 finished visit')
  await toggle.trigger('click')
  await vi.waitFor(() => expect(page.find('[data-visit="s0"]').exists()).toBe(true))
  expect(page.find('[data-visit="s0"]').attributes('href')).toBe('/maintenance/a1?visit=s0')
  expect(page.find('[data-visit="s0"]').text()).toContain('Jahresnachprüfung')
  // no finished visits, no toggle
  expect(page.find('[data-aircraft="a2"] [data-show-past]').exists()).toBe(false)
})

it ('Offers the administration only to maintenance administrators', async () => {
  await login('testuser')
  const userPage = await mountSuspended(MaintenanceIndex)
  expect(userPage.html()).not.toContain('Add aircraft')
  expect(userPage.html()).not.toContain('/maintenance/templates')

  await login('testmaintenanceadmin')
  const adminPage = await mountSuspended(MaintenanceIndex)
  expect(adminPage.html()).toContain('Add aircraft')
  expect(adminPage.html()).toContain('/maintenance/templates')
})
