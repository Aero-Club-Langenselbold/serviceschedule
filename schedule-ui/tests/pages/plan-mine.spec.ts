import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import MyServices from '~/pages/plan/mine.vue'

const mountPage = async () => {
  await useUserStore().login({ username: 'testuser', password: 'howwouldiknow' })
  const page = await mountSuspended(MyServices)
  await vi.waitFor(() => expect(page.html()).toContain('Flugleiter'))
  return page
}

it ('Lists the upcoming services with time, plan and colleagues', async () => {
  const page = await mountPage()
  expect(page.html()).toContain('Saison 2099')
  expect(page.html()).toContain('With Kurt Kollege')
  expect(page.html()).toContain('09:00')
  expect(page.html()).toContain('01:30 PM')
  expect(page.html()).not.toContain('Windenfahrer')
})

it ('Shows past services only on request', async () => {
  const page = await mountPage()
  await page.find('button[role="switch"]').trigger('click')

  await vi.waitFor(() => expect(page.html()).toContain('Windenfahrer'))
  expect(page.findAll('li[data-service]').map(item => item.attributes('data-service'))).toEqual(['e1', 'e2'])
  expect(page.find('li[data-service="e1"]').text()).toContain('Past')
  expect(page.find('li[data-service="e2"]').text()).not.toContain('Past')
})

it ('Downloads the services as calendar file', async () => {
  const page = await mountPage()
  const createObjectURL = vi.fn(() => 'blob:calendar')
  window.URL.createObjectURL = createObjectURL
  window.URL.revokeObjectURL = vi.fn()
  const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
    expect(this.download).toBe('my-services.ics')
  })

  await page.findAll('button').find(button => button.text().includes('Calendar (ICS)'))!.trigger('click')
  await vi.waitFor(() => expect(click).toHaveBeenCalled())
  expect(createObjectURL).toHaveBeenCalled()
  click.mockRestore()
})
