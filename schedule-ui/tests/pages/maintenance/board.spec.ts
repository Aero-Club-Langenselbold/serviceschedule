import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Board from '~/pages/maintenance/[id].vue'

const mountBoard = async (username = 'testuser', route = '/maintenance/a1') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Board, { route, global: { stubs: { ConfirmationDialog: true, MaintenanceTaskDetail: true } } })
  await vi.waitFor(() => expect(page.html()).toContain('Reifen prüfen'))
  return page
}

it ('Lets every user drag cards, except signed off ones', async () => {
  const page = await mountBoard()
  expect(page.find('li[data-task="k1"]').attributes('draggable')).toBe('true')
  expect(page.find('li[data-task="k2"]').attributes('draggable')).toBe('false')
})

it ('Accepts a dragged card only in the other columns and highlights the target', async () => {
  const page = await mountBoard()
  const [open, inProgress] = page.findAll('section')
  await page.find('li[data-task="k1"]').trigger('dragstart')

  // the own column is no drop target: without preventDefault the browser refuses the drop
  const ownColumn = new Event('dragover', { cancelable: true })
  open!.element.dispatchEvent(ownColumn)
  expect(ownColumn.defaultPrevented).toBe(false)

  const otherColumn = new Event('dragover', { cancelable: true })
  inProgress!.element.dispatchEvent(otherColumn)
  expect(otherColumn.defaultPrevented).toBe(true)
  await nextTick()
  expect(inProgress!.classes()).toContain('outline-primary')

  await page.find('li[data-task="k1"]').trigger('dragend')
  expect(inProgress!.classes()).not.toContain('outline-primary')
})

it ('Shows the board of the current workshop visit and every other visit on request', async () => {
  const page = await mountBoard()
  expect(page.html()).toContain('Workshop period:')
  expect(page.find('[data-visit-select]').text()).not.toContain('Jahresnachprüfung')
  expect(page.html()).not.toContain('Reifen wechseln')

  const other = await mountSuspended(Board, { route: '/maintenance/a1?visit=s0', global: { stubs: { ConfirmationDialog: true, MaintenanceTaskDetail: true } } })
  await vi.waitFor(() => expect(other.html()).toContain('Reifen wechseln'))
  expect(other.html()).not.toContain('Reifen prüfen')
  expect(other.find('[data-visit-select]').text()).toContain('Jahresnachprüfung')
})

it ('Lets maintenance administrators choose the templates for a visit', async () => {
  const page = await mountBoard('testmaintenanceadmin')
  const button = page.findAll('button').find(candidate => candidate.text() === 'Create from templates')!
  await button.trigger('click')

  await vi.waitFor(() => expect(document.body.querySelector('[data-template="t1"]')).not.toBeNull())
  const submit = document.body.querySelector('[data-import-submit]') as HTMLButtonElement
  // nothing is preselected
  expect(submit.disabled).toBe(true)
  // the template is already on the board of the visit
  expect(document.body.querySelector('[data-template="t2"]')?.textContent).toContain('Already on the board')
  expect((document.body.querySelector('[data-template="t2"] button') as HTMLButtonElement).disabled).toBe(true)

  ;(document.body.querySelector('[data-template="t1"] button') as HTMLButtonElement).click()
  await vi.waitFor(() => expect(submit.disabled).toBe(false))
  expect(submit.textContent).toContain('Create 1 task')
  page.unmount()
})

it ('Links to the defect book of the aircraft instead of keeping defects per visit', async () => {
  const page = await mountBoard()
  const link = page.find('[data-defect-book]')
  expect(link.attributes('href')).toBe('/maintenance/defects?aircraft=a1')
  // d1 is open, d3 done
  expect(link.text()).toContain('1')
  expect(page.html()).not.toContain('Defects & findings')
})

it ('Opens the task form for a defect coming from the defect book', async () => {
  await mountBoard('testmaintenanceadmin', '/maintenance/a1?newTaskFor=d1')
  await vi.waitFor(() => expect((document.body.querySelector('input[placeholder]') as HTMLInputElement | null)?.value).toBe('Riss in der Haube'))
})
