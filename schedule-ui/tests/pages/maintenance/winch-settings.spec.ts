import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, expect, it, vi } from 'vitest'
import Settings from '~/pages/maintenance/winch/settings.vue'

const stubs = { ConfirmationDialog: true }

const calls = () => ((globalThis as any).__apiCalls ??= []) as { url: string, options: any }[]
const lastCall = (url: string) => calls().filter(call => call.url === url).at(-1)

const mountSettings = async () => {
  await useUserStore().login({ username: 'testmaintenanceadmin', password: 'howwouldiknow' })
  const page = await mountSuspended(Settings, { route: '/maintenance/winch/settings', global: { stubs } })
  await vi.waitFor(() => expect(page.find('[data-option-list="ropes"]').exists()).toBe(true))
  return page
}

const names = (page: any, kind: string) =>
  page.find(`[data-option-list="${kind}"]`).findAll('[data-option-name]').map((input: any) => (input.element as HTMLInputElement).value)

beforeEach(() => { calls().length = 0 })

it ('Lists the ropes and the incidents, inactive ones marked', async () => {
  const page = await mountSettings()
  expect(names(page, 'ropes')).toEqual(['Seil 1', 'Seil 2', 'Altes Seil'])
  expect(names(page, 'incident-types')).toEqual(['Seilriss', 'Schlaufe'])
  expect(page.find('[data-option="r0"] [data-inactive]').exists()).toBe(true)
  expect(page.find('[data-option="r0"] [data-activate]').exists()).toBe(true)
  expect(page.find('[data-option="r1"] [data-activate]').exists()).toBe(false)
})

it ('Adds a rope', async () => {
  const page = await mountSettings()
  const list = page.find('[data-option-list="ropes"]')
  await list.find('[data-new-option]').setValue('Seil 3')
  await list.find('[data-add-option]').trigger('submit')
  await vi.waitFor(() => expect(names(page, 'ropes')).toContain('Seil 3'))
  expect(lastCall('/maintenance/winch/settings/ropes')!.options).toMatchObject({ method: 'POST', body: { name: 'Seil 3' } })
  expect((list.find('[data-new-option]').element as HTMLInputElement).value).toBe('')
})

it ('Renames, reorders and reactivates', async () => {
  const page = await mountSettings()
  const types = page.find('[data-option-list="incident-types"]')
  await types.find('[data-option="wt2"] [data-option-name]').setValue('Schlaufe im Seil')
  await types.find('[data-option="wt2"] [data-save-option]').trigger('click')
  await vi.waitFor(() => expect(names(page, 'incident-types')).toEqual(['Seilriss', 'Schlaufe im Seil']))
  expect(lastCall('/maintenance/winch/settings/incident-types/wt2')!.options).toMatchObject({ method: 'PUT', body: { name: 'Schlaufe im Seil', active: true } })

  await page.find('[data-option="r2"] [data-move-up]').trigger('click')
  await vi.waitFor(() => expect(names(page, 'ropes')).toEqual(['Seil 2', 'Seil 1', 'Altes Seil']))
  expect(lastCall('/maintenance/winch/settings/ropes/order')!.options.body).toEqual({ ids: ['r2', 'r1', 'r0'] })
  expect(page.find('[data-option="r2"] [data-move-up]').attributes('disabled')).toBeDefined()

  await page.find('[data-option="r0"] [data-activate]').trigger('click')
  await vi.waitFor(() => expect(lastCall('/maintenance/winch/settings/ropes/r0')!.options.body).toEqual({ name: 'Altes Seil', active: true }))
})

it ('Explains that used entries are only deactivated', async () => {
  const page = await mountSettings()
  expect(page.find('[data-option="r1"] confirmation-dialog-stub').attributes('confirmlabel')).toBe('Stop using')
  expect(page.find('[data-option="r2"] confirmation-dialog-stub').attributes('confirmlabel')).toBe('Delete')
})
