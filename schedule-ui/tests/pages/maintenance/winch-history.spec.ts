import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, expect, it, vi } from 'vitest'
import History from '~/pages/maintenance/winch/history.vue'

const calls = () => ((globalThis as any).__apiCalls ??= []) as { url: string, options: any }[]

const mountHistory = async (route = '/maintenance/winch/history') => {
  await useUserStore().login({ username: 'testuser', password: 'howwouldiknow' })
  const page = await mountSuspended(History, { route })
  await vi.waitFor(() => expect(page.find('[data-open-day]').exists()).toBe(true))
  return page
}

beforeEach(() => { calls().length = 0 })

it ('Lists the days of the current year by default', async () => {
  const page = await mountHistory()
  const request = calls().find(call => call.url.startsWith('/maintenance/winch/days?'))!
  expect(new URLSearchParams(request.url.split('?')[1]).get('year')).toBe(String(new Date().getFullYear()))
  expect(new URLSearchParams(request.url.split('?')[1]).get('page')).toBe('0')

  expect(page.findAll('[data-open-day]')).toHaveLength(2)
  expect(page.text()).toContain('Albers, Anna, testuser')
  expect(page.text()).toContain('Seilriss × 2')
  expect(page.text()).toContain('No incidents')
  expect(page.find('[data-open-day="2026-11-20"]').attributes('href')).toBe('/maintenance/winch/days/2026-11-20')
})

it ('Shows another year from the URL', async () => {
  const page = await mountHistory('/maintenance/winch/history?year=2024')
  expect(page.findAll('[data-open-day]').map(link => link.attributes('data-open-day'))).toEqual(['2024-05-01'])
  expect(page.text()).toContain('Schlaufe × 1')
  expect(calls().some(call => call.url === '/maintenance/winch/years')).toBe(true)
})
