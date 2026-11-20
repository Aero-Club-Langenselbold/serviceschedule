import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Board from '~/pages/maintenance/[id].vue'

const mountBoard = async () => {
  await useUserStore().login({ username: 'testuser', password: 'howwouldiknow' })
  const page = await mountSuspended(Board, { route: '/maintenance/a1', global: { stubs: { ConfirmationDialog: true, MaintenanceTaskDetail: true } } })
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
