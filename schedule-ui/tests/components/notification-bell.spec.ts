import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import NotificationBell from '~/components/notification-bell.vue'

const mockNavigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => mockNavigateTo)

beforeEach(async () => {
  mockNavigateTo.mockClear()
  await useUserStore().login({ username: 'testuser', password: 'howwouldiknow' })
})

const mountBell = async () => {
  const wrapper = await mountSuspended(NotificationBell)
  await flushPromises()
  return wrapper
}
const openList = async (wrapper: Awaited<ReturnType<typeof mountBell>>) => {
  await wrapper.find('[data-notification-bell]').trigger('click')
  await vi.waitFor(() => expect(document.body.querySelector('[data-notification-list]')).not.toBeNull())
}

it ('Shows the number of unread notifications at the bell', async () => {
  const wrapper = await mountBell()
  expect(wrapper.find('[data-notification-bell]').text()).toContain('1')
  expect(wrapper.find('[data-notification-bell]').attributes('aria-label')).toContain('1 unread')
  wrapper.unmount()
})

it ('Lists the notifications with the assigned defect', async () => {
  const wrapper = await mountBell()
  await openList(wrapper)
  const list = document.body.querySelector('[data-notification-list]')!
  expect(list.textContent).toContain('testadmin assigned a defect to you')
  expect(list.textContent).toContain('D-1234 · Riss in der Haube')
  // without a known actor
  expect(list.textContent).toContain('Someone assigned a defect to you')
  wrapper.unmount()
})

it ('Jumps to the defect and marks the notification read', async () => {
  const wrapper = await mountBell()
  await openList(wrapper)
  ;(document.body.querySelector('[data-notification="n1"]') as HTMLButtonElement).click()
  await flushPromises()

  expect(mockNavigateTo).toHaveBeenCalledWith({ path: '/maintenance/defects', query: { defect: 'd1' } })
  expect(wrapper.find('[data-notification-bell]').attributes('aria-label')).toContain('No unread')
  wrapper.unmount()
})

it ('Marks all notifications read at once', async () => {
  const wrapper = await mountBell()
  await openList(wrapper)
  ;(document.body.querySelector('[data-mark-all-read]') as HTMLButtonElement).click()
  await flushPromises()

  expect(wrapper.find('[data-notification-bell]').attributes('aria-label')).toContain('No unread')
  wrapper.unmount()
})

it ('Lists a submitted checklist and opens it', async () => {
  const wrapper = await mountBell()
  await openList(wrapper)
  const list = document.body.querySelector('[data-notification-list]')!
  expect(list.textContent).toContain('Albers, Anna confirmed a checklist for approval')
  expect(list.textContent).toContain('D-1234 · Start of operations')

  document.body.querySelector<HTMLButtonElement>('[data-notification="n2"]')!.click()
  await vi.waitFor(() => expect(mockNavigateTo).toHaveBeenCalledWith('/operations/checklists/c1'))
  wrapper.unmount()
})
