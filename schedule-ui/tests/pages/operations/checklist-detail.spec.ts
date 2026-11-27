import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import Detail from '~/pages/operations/checklists/[id].vue'

const mountDetail = async (id: string, username = 'testoperationsadmin') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Detail, { route: `/operations/checklists/${id}` })
  await flushPromises()
  return page
}

it ('Operations admins approve a submitted checklist from the notification', async () => {
  const page = await mountDetail('c-submitted')
  await vi.waitFor(() => expect(page.find('[data-approve]').exists()).toBe(true))
  expect(page.find('[data-checklist-status]').text()).toBe('Awaiting approval')
  expect(page.find('[data-submitted-by]').text()).toContain('Albers, Anna')
  expect(page.find('[data-submit]').exists()).toBe(false)

  await page.find('[data-approve]').trigger('click')
  await vi.waitFor(() => expect(page.find('[data-checklist-status]').text()).toBe('Approved'))
  expect(page.find('[data-approve]').exists()).toBe(false)
})

it ('Operations admins send a checklist back', async () => {
  const page = await mountDetail('c-submitted')
  await vi.waitFor(() => expect(page.find('[data-reopen]').exists()).toBe(true))
  await page.find('[data-reopen]').trigger('click')
  await vi.waitFor(() => expect(page.find('[data-checklist-status]').text()).toBe('In progress'))
  expect(page.find('[data-submit]').exists()).toBe(true)
})

it ('Members cannot read checklists of past days', async () => {
  const page = await mountDetail('c-old', 'testuser')
  await vi.waitFor(() => expect(page.text()).toContain('Checklists of past days are for operations admins only'))
  expect(page.find('[data-checklist-header]').exists()).toBe(false)
})
