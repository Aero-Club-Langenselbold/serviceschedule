import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Schedule from '~/pages/maintenance/schedule.vue'

const mountSchedule = async (username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Schedule, { global: { stubs: { MaintenanceWorkshopSlotForm: true } } })
  await vi.waitFor(() => expect(page.find('[data-slot="s1"]').exists()).toBe(true))
  return page
}

it ('Shows a row per aircraft with the checklist progress on the current period', async () => {
  const page = await mountSchedule()
  const rows = page.findAll('[data-row]')
  // aircraft are ordered by their first period
  expect(rows.map(row => row.attributes('data-row'))).toEqual(['a1', 'a2'])
  expect(rows[0]!.text()).toContain('3/8 steps')

  // 3 of 8 steps: rounded down
  expect(page.find('[data-slot="s1"]').text()).toBe('D-1234 · 37 %')
  expect(page.find('[data-slot="s1"] span').attributes('style')).toContain('width: 37%')
  // periods of different aircraft overlap, later periods carry their note
  expect(page.find('[data-slot="s2"]').text()).toBe('Lackierung')
})

it ('Hides past periods until asked for', async () => {
  const page = await mountSchedule()
  expect(page.find('[data-slot="s0"]').exists()).toBe(false)
  expect(page.html()).toContain('26 days')

  await page.find('button[role="switch"]').trigger('click')
  await vi.waitFor(() => expect(page.find('[data-slot="s0"]').exists()).toBe(true))
  expect(page.html()).toContain('Jahresnachprüfung')
})

it ('Lets only maintenance administrators plan periods', async () => {
  const userPage = await mountSchedule()
  expect(userPage.html()).not.toContain('Plan workshop period')
  expect(userPage.find('[aria-label="Edit workshop period"]').exists()).toBe(false)

  const adminPage = await mountSchedule('testmaintenanceadmin')
  expect(adminPage.html()).toContain('Plan workshop period')
  expect(adminPage.findAll('[aria-label="Edit workshop period"]')).toHaveLength(2)
})
