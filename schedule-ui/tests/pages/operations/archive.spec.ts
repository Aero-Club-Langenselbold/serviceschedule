import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Archive from '~/pages/operations/archive.vue'

const calls = () => (globalThis as any).__apiCalls as { url: string, options: any }[]
const archiveQueries = () => calls().filter(call => call.url.startsWith('/operations/checklists?')).map(call => new URLSearchParams(call.url.split('?')[1]))

it ('Lists the checklists of all days with their result', async () => {
  await useUserStore().login({ username: 'testoperationsadmin', password: 'howwouldiknow' })
  const page = await mountSuspended(Archive, { route: '/operations/archive' })
  await vi.waitFor(() => expect(page.findAll('[data-open-checklist]').length).toBe(2))

  expect(page.text()).toContain('D-1234 · ASK 21')
  expect(page.text()).toContain('Start of operations')
  expect(page.text()).toContain('Approved')
  expect(page.text()).toContain('2 of 2 checked · 1 not OK')
  expect(page.find('[data-open-checklist="c0"]').attributes('href')).toBe('/operations/checklists/c0')
  // the first page, latest first as decided by the backend
  expect(archiveQueries().at(-1)!.get('page')).toBe('0')
})

it ('Searches by text', async () => {
  await useUserStore().login({ username: 'testoperationsadmin', password: 'howwouldiknow' })
  const page = await mountSuspended(Archive, { route: '/operations/archive' })
  await vi.waitFor(() => expect(page.findAll('[data-open-checklist]').length).toBe(2))

  await page.find('input[data-filter-search]').setValue('Riss')
  await vi.waitFor(() => expect(archiveQueries().at(-1)!.get('search')).toBe('Riss'), { timeout: 2000 })
})
