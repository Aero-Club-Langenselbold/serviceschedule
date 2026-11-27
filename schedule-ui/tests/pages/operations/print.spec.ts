import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Print from '~/pages/operations/print/[aircraftId]/[kind].vue'

it ('Prints the registration as heading and the QR code of the fixed link below', async () => {
  await useUserStore().login({ username: 'testoperationsadmin', password: 'howwouldiknow' })
  const page = await mountSuspended(Print, { route: '/operations/print/a1/end' })
  await vi.waitFor(() => expect(page.find('[data-qr] svg').exists()).toBe(true))

  expect(page.find('[data-print-title]').text()).toBe('D-1234')
  expect(page.text()).toContain('End of operations')
  expect(page.text()).toContain('always opens the checklist of the current day')

  // the test DOM has no print dialog
  const print = vi.fn()
  window.print = print
  await page.find('[data-print-button]').trigger('click')
  expect(print).toHaveBeenCalled()
})

it ('Encodes the link to the checklist of the current day, never a fixed checklist', async () => {
  const { todayUrl } = useOperations()
  expect(todayUrl('a1', 'END')).toMatch(/\/operations\/checklist\/a1\/end$/)
  expect(todayPath('a1', 'START')).toBe('/operations/checklist/a1/start')
  expect(parseKind('start')).toBe('START')
  expect(parseKind('END')).toBe('END')
  expect(parseKind('lunch')).toBeNull()
})
