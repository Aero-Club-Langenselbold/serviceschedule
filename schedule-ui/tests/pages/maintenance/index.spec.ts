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
  expect(page.html()).toContain('No workshop period')
  expect(page.html()).toContain('/maintenance/a1')
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
