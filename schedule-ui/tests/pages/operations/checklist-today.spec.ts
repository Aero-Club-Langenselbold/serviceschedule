import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import Today from '~/pages/operations/checklist/[aircraftId]/[kind].vue'

const mountToday = async (route: string, username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Today, { route })
  await flushPromises()
  return page
}

it ('The fixed link opens the checklist of today', async () => {
  const page = await mountToday('/operations/checklist/a1/start')
  await vi.waitFor(() => expect(page.find('[data-checklist-header]').exists()).toBe(true))
  expect(page.text()).toContain('Start of operations')
  expect(page.text()).toContain('D-1234 · ASK 21')
  expect(page.find('[data-created-by]').text()).toContain('Albers, Anna')
  expect(page.findAll('[data-item]')).toHaveLength(2)
})

it ('Offers to start the checklist of today, which records the starter', async () => {
  const page = await mountToday('/operations/checklist/a1/end')
  await vi.waitFor(() => expect(page.find('[data-start]').exists()).toBe(true))
  expect(page.find('[data-start]').text()).toContain('Abdeckung drauf')
  expect(page.find('[data-start]').text()).toContain('recorded as the recorder')

  await page.find('[data-start-button]').trigger('click')
  await vi.waitFor(() => expect(page.find('[data-checklist-header]').exists()).toBe(true))
  expect(page.find('[data-created-by]').text()).toContain('testuser')
  expect(page.findAll('[data-item]')).toHaveLength(1)
})

it ('Tells when an aircraft has no such checklist', async () => {
  const page = await mountToday('/operations/checklist/a2/start')
  await vi.waitFor(() => expect(page.find('[data-no-template]').exists()).toBe(true))
})

it ('Shows an error for unknown aircraft and kinds', async () => {
  expect((await mountToday('/operations/checklist/missing/start')).text()).toContain('Checklist not found')
  expect((await mountToday('/operations/checklist/a1/lunch')).text()).toContain('Checklist not found')
})

it ('Offers the QR code to those who maintain the checklists', async () => {
  expect((await mountToday('/operations/checklist/a1/start')).text()).not.toContain('Print QR code')
  expect((await mountToday('/operations/checklist/a1/start', 'testoperationsadmin')).text()).toContain('Print QR code')
})
