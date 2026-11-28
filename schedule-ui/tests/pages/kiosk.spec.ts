import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it } from 'vitest'
import Kiosk from '~/pages/kiosk.vue'

it ('Shows large tiles for the areas of the kiosk mode', async () => {
  await useUserStore().loginKiosk({ username: 'anna@example.org', pin: '123456' })
  const page = await mountSuspended(Kiosk, { route: '/kiosk' })

  expect(page.text()).toContain('anna@example.org')
  const tiles = page.findAll('[data-kiosk-tiles] a').map(tile => tile.attributes('href'))
  expect(tiles).toEqual(['/operations', '/maintenance', '/maintenance/defects', '/maintenance/schedule', '/maintenance/inventory', '/maintenance/winch'])
  expect(page.text()).toContain('Checklists')
  expect(page.text()).toContain('Report and work on defects')
  expect(page.html()).not.toContain('/request/list')
})
