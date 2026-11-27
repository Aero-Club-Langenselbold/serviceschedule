import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Defects from '~/pages/maintenance/defects.vue'

const stubs = { ConfirmationDialog: true }

const mountDefects = async (route = '/maintenance/defects', username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Defects, { route, global: { stubs } })
  await vi.waitFor(() => expect(page.find('[data-aircraft-filter]').exists()).toBe(true))
  await vi.waitFor(() => expect(page.findAll('[data-defect]').length).toBeGreaterThan(0))
  return page
}
const shownIds = (page: Awaited<ReturnType<typeof mountDefects>>) => page.findAll('[data-defect]').map(item => item.attributes('data-defect'))

it ('Lists the open defects of all aircraft and equipment, the most urgent first', async () => {
  const page = await mountDefects()
  expect(shownIds(page)).toEqual(['d1', 'd2'])
  expect(page.text()).toContain('D-1234 · ASK 21')
  expect(page.text()).toContain('Winde 1 · Winde')
  expect(page.text()).toContain('Assigned to me')
  expect(page.text()).toContain('In progress')
  expect(page.text()).toContain('1 comment')
})

it ('Counts the defects per status and switches between them', async () => {
  const page = await mountDefects()
  expect(page.find('[data-status-filter="OPEN"]').text()).toContain('2')
  expect(page.find('[data-status-filter="CLOSED"]').text()).toContain('1')
  expect(page.find('[data-status-filter="SIGNED"]').text()).toContain('1')
  expect(page.find('[data-status-filter="ALL"]').text()).toContain('4')

  const waiting = await mountDefects('/maintenance/defects?status=CLOSED')
  expect(shownIds(waiting)).toEqual(['d3'])
  expect(waiting.text()).toContain('Done')

  const signed = await mountDefects('/maintenance/defects?status=SIGNED')
  expect(shownIds(signed)).toEqual(['d4'])
  expect(signed.text()).toContain('Signed off')
})

it ('Filters by aircraft or equipment like the workshop schedule', async () => {
  const page = await mountDefects('/maintenance/defects?aircraft=a1&status=ALL')
  expect(shownIds(page)).toEqual(['d1', 'd3'])
  expect(page.find('[data-aircraft-filter]').text()).toContain('D-1234 · ASK 21')
  expect(page.find('[data-status-filter="ALL"]').text()).toContain('2')
})

it ('Shows only the defects assigned to me on request', async () => {
  const page = await mountDefects('/maintenance/defects?mine=1')
  expect(shownIds(page)).toEqual(['d1'])
})

it ('Opens the defect from the URL, e.g. coming from a notification', async () => {
  await mountDefects('/maintenance/defects?defect=d1')
  await vi.waitFor(() => expect(document.body.querySelector('[data-activity]')?.textContent).toContain('Bitte vor dem Wochenende anschauen'))
  expect(document.body.querySelector('[data-assignees]')?.textContent).toContain('testuser')
})

it ('Offers the assignment only to maintenance administrators when reporting a defect', async () => {
  const userPage = await mountDefects()
  await userPage.findAll('button').find(button => button.text() === 'Report defect')!.trigger('click')
  await vi.waitFor(() => expect(document.body.querySelector('[data-aircraft-select]')).not.toBeNull())
  expect(document.body.querySelector('[data-assignee-select]')).toBeNull()
  userPage.unmount()

  const adminPage = await mountDefects('/maintenance/defects', 'testmaintenanceadmin')
  await adminPage.findAll('button').find(button => button.text() === 'Report defect')!.trigger('click')
  await vi.waitFor(() => expect(document.body.querySelector('[data-assignee-select]')).not.toBeNull())
})
