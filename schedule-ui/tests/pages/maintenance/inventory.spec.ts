import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Inventory from '~/pages/maintenance/inventory.vue'

// The tooltip of the confirmation dialog needs the provider of UApp, which the test mount lacks.
const stubs = { ConfirmationDialog: true, MaintenanceInventoryForm: true, MaintenanceInventoryHistory: true }

const mountInventory = async (username = 'testuser', route = '/maintenance/inventory') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Inventory, { route, global: { stubs } })
  await vi.waitFor(() => expect(page.find('[data-item]').exists()).toBe(true))
  return page
}

it ('Lists the consumables with stock, location and minimum', async () => {
  const page = await mountInventory()
  expect(page.findAll('[data-item]').map(item => item.attributes('data-item'))).toEqual(['i1', 'i2'])
  const politur = page.find('[data-item="i1"]')
  expect(politur.text()).toContain('Running low')
  expect(politur.text()).toContain('Regal A')
  expect(politur.text()).toContain('Min. 2 Dosen')
  expect(page.find('[data-item="i2"]').text()).toContain('Enough')
})

it ('Shows the spare parts with the aircraft types they fit and filters them', async () => {
  const page = await mountInventory('testuser', '/maintenance/inventory?kind=SPARE_PART')
  expect(page.findAll('[data-item]')).toHaveLength(3)
  expect(page.find('[data-item="i3"]').text()).toContain('ASK 21')
  expect(page.find('[data-item="i3"]').text()).toContain('Empty')
  expect(page.find('[data-item="i4"]').text()).toContain('All aircraft')

  await page.find('input[placeholder="Search items"]').setValue('brems')
  expect(page.findAll('[data-item]').map(item => item.attributes('data-item'))).toEqual(['i5'])

  await page.find('input[placeholder="Search items"]').setValue('')
  await page.find('button[role="switch"]').trigger('click')
  expect(page.findAll('[data-item]').map(item => item.attributes('data-item'))).toEqual(['i3'])
})

it ('Lets every user count a fill level', async () => {
  const page = await mountInventory()
  const low = page.find('[data-item="i2"]').findAll('button').find(button => button.text() === 'Low')!
  await low.trigger('click')
  expect(low.attributes('aria-pressed')).toBe('true')
  await vi.waitFor(() => expect(page.find('[data-item="i2"]').text()).toContain('Running low'))
})

it ('Offers the administration only to maintenance administrators', async () => {
  const userPage = await mountInventory()
  expect(userPage.html()).not.toContain('Add item')
  expect(userPage.find('[aria-label="Edit item"]').exists()).toBe(false)
  expect(userPage.find('[aria-label="History"]').exists()).toBe(true)

  const adminPage = await mountInventory('testmaintenanceadmin')
  expect(adminPage.html()).toContain('Add item')
  expect(adminPage.find('[aria-label="Edit item"]').exists()).toBe(true)
})
