import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Schedule from '~/pages/maintenance/schedule.vue'

const mountSchedule = async (username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Schedule, { global: { stubs: { MaintenanceWorkshopSlotForm: true } } })
  await vi.waitFor(() => expect(page.find('[data-slot="s1"]').exists()).toBe(true))
  return page
}

it ('Shows a row per aircraft and the checklist progress of every visit on its bar', async () => {
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

it ('Shows the progress of past visits on their own bars', async () => {
  const page = await mountSchedule()
  await page.find('button[role="switch"]').trigger('click')
  await vi.waitFor(() => expect(page.find('[data-slot="s0"]').exists()).toBe(true))
  expect(page.find('[data-slot="s0"]').text()).toBe('Jahresnachprüfung · 100 %')
  // the row keeps the progress of the current visit
  expect(page.find('[data-row="a1"]').text()).toContain('3/8 steps')
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

const openPanel = async (page: Awaited<ReturnType<typeof mountSchedule>>, slot: string) => {
  await page.find(`[data-slot="${slot}"]`).trigger('click')
  await vi.waitFor(() => expect(document.body.querySelector(`[data-slot-panel="${slot}"]`)).not.toBeNull())
  return document.body.querySelector(`[data-slot-panel="${slot}"]`) as HTMLElement
}

it ('Opens a dropdown below a bar with the period and a link to the board of the visit', async () => {
  const page = await mountSchedule()
  const panel = await openPanel(page, 's1')

  expect(panel.textContent).toContain('D-1234')
  expect(panel.textContent).toContain('37 % of the checklist steps done')
  expect(panel.textContent).toContain('1 of 4 done')
  expect(panel.querySelector('a[href="/maintenance/a1?visit=s1"]')?.textContent).toContain('Open board')
  // members only look
  expect(panel.querySelector('input')).toBeNull()
  page.unmount()
})

it ('Lets maintenance administrators change the period in the dropdown', async () => {
  const page = await mountSchedule('testmaintenanceadmin')
  const panel = await openPanel(page, 's2')

  const inputs = panel.querySelectorAll('input')
  expect(inputs).toHaveLength(3)
  expect((inputs[2] as HTMLInputElement).value).toBe('Lackierung')
  const save = [...panel.querySelectorAll('button')].find(button => button.textContent?.includes('Save'))!
  // nothing changed yet
  expect(save.disabled).toBe(true)

  inputs[2]!.value = 'Lackierung und Wachs'
  inputs[2]!.dispatchEvent(new Event('input'))
  await vi.waitFor(() => expect(save.disabled).toBe(false))
  expect(panel.querySelector('a[href="/maintenance/a2?visit=s2"]')).not.toBeNull()
  expect(panel.textContent).toContain('No tasks yet')
  page.unmount()
})
