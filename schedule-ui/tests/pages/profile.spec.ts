import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import Profile from '~/pages/profile.vue'

const calls = () => (globalThis as any).__apiCalls as { url: string, options: any }[]
const stubs = { ConfirmationDialog: true }

const typePin = async (page: Awaited<ReturnType<typeof mountSuspended>>, selector: string, pin: string) => {
  const inputs = page.findAll(`${selector} input[data-slot="base"]`)
  expect(inputs).toHaveLength(6)
  for (const [index, digit] of [...pin].entries()) {
    await inputs[index]!.setValue(digit)
  }
}

const mountProfile = async () => {
  await useUserStore().login({ username: 'testuser', password: 'howwouldiknow' })
  const page = await mountSuspended(Profile, { route: '/profile', global: { stubs } })
  await vi.waitFor(() => expect(page.find('[data-kiosk-state]').exists()).toBe(true))
  return page
}

it ('Shows whether a kiosk code is set, never the code itself', async () => {
  const page = await mountProfile()
  expect(page.text()).toContain('My settings')
  expect(page.text()).toContain('testuser')
  expect(page.find('[data-kiosk-state]').text()).toBe('Not set')
  expect(page.text()).toContain('cannot be shown again')
  // masked like a password
  page.findAll('[data-pin] input[data-slot="base"]').forEach(input => expect(input.attributes('type')).toBe('password'))
})

it ('Saves a 6-digit PIN entered twice and forgets it right away', async () => {
  const page = await mountProfile()
  expect(page.find('[data-save-pin]').attributes('disabled')).toBeDefined()

  await typePin(page, '[data-pin]', '012345')
  await typePin(page, '[data-pin-confirm]', '012345')
  await vi.waitFor(() => expect(page.find('[data-save-pin]').attributes('disabled')).toBeUndefined())
  await page.find('form').trigger('submit')
  await flushPromises()

  const call = calls().filter(entry => entry.url === '/user/me/kiosk-pin').at(-1)!
  // leading zeros stay, the PIN is a string of digits
  expect(call.options).toEqual({ method: 'PUT', body: { pin: '012345' } })
  expect(page.find('[data-kiosk-state]').text()).toBe('Set')
  page.findAll('[data-pin] input[data-slot="base"]').forEach(input => expect((input.element as HTMLInputElement).value).toBe(''))
})

it ('Does not save PINs that do not match', async () => {
  const page = await mountProfile()
  await typePin(page, '[data-pin]', '123456')
  await typePin(page, '[data-pin-confirm]', '123457')
  await vi.waitFor(() => expect(page.text()).toContain('The PINs do not match'))
  expect(page.find('[data-save-pin]').attributes('disabled')).toBeDefined()
})
