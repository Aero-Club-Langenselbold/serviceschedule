import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import Winch from '~/pages/maintenance/winch/index.vue'

// The tooltip of the confirmation dialog needs the provider of UApp, which the test mount lacks.
const stubs = { ConfirmationDialog: true }

const calls = () => ((globalThis as any).__apiCalls ??= []) as { url: string, options: any }[]
const lastCall = (url: string) => calls().filter(call => call.url === url).at(-1)

const mountWinch = async (username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Winch, { route: '/maintenance/winch', global: { stubs } })
  await vi.waitFor(() => expect(page.find('[data-winch-header]').exists()).toBe(true))
  return page
}

beforeEach(() => {
  calls().length = 0
  // jsdom has no object URLs for the blobs of the photos
  URL.createObjectURL = vi.fn(() => 'blob:photo')
  URL.revokeObjectURL = vi.fn()
})

it ('Shows the page of today with the winch drivers and every rope', async () => {
  const page = await mountWinch()
  expect(page.find('[data-winch-header]').text()).toContain('Today')
  expect(page.find('[data-drivers]').text()).toContain('Albers, Anna, testuser')
  expect(page.findAll('[data-rope]').map(rope => rope.attributes('data-rope'))).toEqual(['r1', 'r2'])

  const seil1 = page.find('[data-rope="r1"]')
  expect(seil1.text()).toContain('Seil 1')
  expect(seil1.find('[data-incident-counts]').text()).toContain('Seilriss × 1')
  expect(seil1.find('[data-incident="wi1"]').text()).toContain('Winch driver: testuser')
  expect(seil1.find('[data-incident="wi1"]').text()).toContain('am Startplatz')
  // the photo is loaded with the token and offered for deletion only to its uploader
  await vi.waitFor(() => expect(seil1.find('[data-photo="wp1"] img').exists()).toBe(true))
  expect(seil1.find('[data-photo="wp1"] [data-delete-photo]').exists()).toBe(false)
  expect(lastCall('/maintenance/winch/photos/wp1')!.options.responseType).toBe('blob')

  expect(page.find('[data-rope="r2"]').text()).toContain('No incidents.')
})

it ('Shows the ropes side by side and the remark of the day below them', async () => {
  const page = await mountWinch()
  const ropes = page.find('[data-ropes]')
  expect(ropes.classes()).toContain('md:grid-cols-2')
  expect(ropes.findAll('[data-rope]')).toHaveLength(2)
  const html = page.html()
  expect(html.indexOf('data-day-remark')).toBeGreaterThan(html.indexOf('data-ropes'))
})

it ('Records an incident of a rope with an optional comment', async () => {
  const page = await mountWinch()
  const buttons = page.find('[data-rope="r2"]').findAll('[data-add-incident]')
  expect(buttons.map(button => button.text())).toEqual(['Seilriss', 'Schlaufe'])

  await page.find('[data-rope="r2"] [data-add-incident="wt2"]').trigger('click')
  await vi.waitFor(() => expect(document.querySelector('[data-incident-form]')).not.toBeNull())
  expect(document.body.textContent).toContain('Record Schlaufe on Seil 2')
  const comment = document.querySelector('[data-incident-comment]') as HTMLTextAreaElement
  comment.value = ' am Seilende '
  comment.dispatchEvent(new Event('input'))
  ;(document.querySelector('[data-confirm-incident]') as HTMLButtonElement).click()

  await vi.waitFor(() => expect(page.find('[data-rope="r2"] [data-incident="wi-new"]').exists()).toBe(true))
  expect(lastCall('/maintenance/winch/incidents')!.options).toMatchObject({ method: 'POST', body: { ropeId: 'r2', typeId: 'wt2', comment: 'am Seilende' } })
  expect(page.find('[data-rope="r2"]').text()).toContain('Schlaufe × 1')
})

it ('Saves the remark of the day with the version it was loaded with', async () => {
  const page = await mountWinch()
  const save = page.find('[data-save-day-remark]')
  expect(save.attributes('disabled')).toBeDefined()
  expect(page.find('[data-day-remark]').text()).toContain('Last changed by Albers, Anna')

  await page.find('[data-day-remark-input]').setValue('Winde läuft rund, 42 Starts')
  await page.find('[data-save-day-remark]').trigger('click')
  await flushPromises()
  expect(lastCall(`/maintenance/winch/days/${winchToday()}/remark`)!.options).toMatchObject({ method: 'PUT', body: { text: 'Winde läuft rund, 42 Starts', version: 3 } })
  await vi.waitFor(() => expect(page.find('[data-day-remark]').text()).toContain('Last changed by testuser'))
})

it ('Saves the remark of a rope', async () => {
  const page = await mountWinch()
  const seil1 = page.find('[data-rope="r1"]')
  await seil1.find('[data-rope-remark-input]').setValue('Neu gespleißt')
  await seil1.find('[data-save-rope-remark]').trigger('click')
  await flushPromises()
  expect(lastCall(`/maintenance/winch/days/${winchToday()}/ropes/r1/remark`)!.options).toMatchObject({ method: 'PUT', body: { text: 'Neu gespleißt', version: null } })
})

it ('Reloads the page when somebody else changed the remark in the meantime', async () => {
  const page = await mountWinch()
  calls().length = 0
  await page.find('[data-day-remark-input]').setValue('Konflikt')
  await page.find('[data-save-day-remark]').trigger('click')
  await vi.waitFor(() => expect(lastCall('/maintenance/winch/days/today')).toBeDefined())
  await vi.waitFor(() => expect((page.find('[data-day-remark-input]').element as HTMLTextAreaElement).value).toBe('Winde läuft rund'))
})

it ('Uploads photos of a rope', async () => {
  const page = await mountWinch()
  await page.find('[data-rope="r2"] [data-add-photo]').trigger('click')
  const input = page.find('[data-photo-input]')
  const file = new File(['jpeg'], 'riss.jpg', { type: 'image/jpeg' })
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')

  await vi.waitFor(() => expect(page.find('[data-rope="r2"] [data-photo="wp-new"]').exists()).toBe(true))
  const upload = lastCall('/maintenance/winch/ropes/r2/photos')!
  expect(upload.options.method).toBe('POST')
  expect((upload.options.body as FormData).get('file')).toBeInstanceOf(File)
  expect(page.find('[data-rope="r2"] [data-photo="wp-new"] [data-delete-photo]').exists()).toBe(true)
})

it ('Rejects files that are no images before uploading them', async () => {
  const page = await mountWinch()
  calls().length = 0
  await page.find('[data-rope="r2"] [data-add-photo]').trigger('click')
  const input = page.find('[data-photo-input]')
  Object.defineProperty(input.element, 'files', { value: [new File(['text'], 'notiz.txt', { type: 'text/plain' })], configurable: true })
  await input.trigger('change')
  await flushPromises()
  expect(calls().some(call => call.url.endsWith('/photos'))).toBe(false)
})

it ('Links the earlier entries for everybody and the settings for maintenance administrators', async () => {
  const userPage = await mountWinch()
  expect(userPage.find('[data-history-link]').exists()).toBe(true)
  expect(userPage.find('[data-settings-link]').exists()).toBe(false)

  const adminPage = await mountWinch('testmaintenanceadmin')
  expect(adminPage.find('[data-settings-link]').exists()).toBe(true)
})

function winchToday() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
