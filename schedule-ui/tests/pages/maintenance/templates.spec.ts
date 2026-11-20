import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Templates from '~/pages/maintenance/templates.vue'

it ('Lists the templates with the aircraft types they apply to', async () => {
  await useUserStore().login({ username: 'testmaintenanceadmin', password: 'howwouldiknow' })
  // The tooltip of the confirmation dialog needs the provider of UApp, which the test mount lacks.
  const page = await mountSuspended(Templates, { global: { stubs: { ConfirmationDialog: true } } })

  await vi.waitFor(() => expect(page.html()).toContain('Rumpf reinigen'))
  expect(page.html()).toContain('All aircraft')
  expect(page.html()).toContain('2 steps')
  expect(page.html()).toContain('1 h')
  expect(page.html()).toContain('Haube polieren')
  expect(page.html()).toContain('ASK 21')
})
