import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import WinchDayPage from '~/pages/maintenance/winch/days/[date].vue'

const stubs = { ConfirmationDialog: true }

const mountDay = async (date: string, username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  return mountSuspended(WinchDayPage, { route: `/maintenance/winch/days/${date}`, global: { stubs } })
}

it ('Shows an earlier day read only', async () => {
  const page = await mountDay('2026-11-20')
  await vi.waitFor(() => expect(page.find('[data-winch-header]').exists()).toBe(true))
  expect(page.text()).toContain('Winch book of 11/20/2026')
  expect(page.find('[data-day-remark-text]').text()).toBe('Winde läuft rund')
  expect(page.find('[data-day-remark-input]').exists()).toBe(false)
  expect(page.find('[data-add-incident]').exists()).toBe(false)
  expect(page.find('[data-add-photo]').exists()).toBe(false)
  expect(page.findAll('[data-rope]')).toHaveLength(1)
  expect(page.find('[data-incident="wi1"]').text()).toContain('Seilriss')
  expect(page.find('confirmation-dialog-stub').exists()).toBe(false)
})

it ('Lets maintenance administrators correct the remarks of an earlier day', async () => {
  const page = await mountDay('2026-11-21', 'testmaintenanceadmin')
  await vi.waitFor(() => expect(page.find('[data-day-remark-input]').exists()).toBe(true))
  expect(page.find('[data-add-incident]').exists()).toBe(false)
  expect(page.find('[data-rope="r1"] confirmation-dialog-stub').exists()).toBe(true)
})

it ('Tells when there is no entry for a day', async () => {
  const page = await mountDay('2026-01-01')
  await vi.waitFor(() => expect(page.text()).toContain('There is no winch book entry for this day.'))
  expect((await mountDay('gestern')).text()).toContain('There is no winch book entry for this day.')
})
