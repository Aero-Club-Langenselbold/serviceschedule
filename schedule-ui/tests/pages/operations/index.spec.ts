import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it, vi } from 'vitest'
import Operations from '~/pages/operations/index.vue'

const stubs = { ConfirmationDialog: true }

const mountOverview = async (username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const page = await mountSuspended(Operations, { route: '/operations', global: { stubs } })
  await vi.waitFor(() => expect(page.findAll('[data-aircraft]').length).toBe(2))
  return page
}

it ('Shows the start and end of operations checklists of every aircraft with the state of today', async () => {
  const page = await mountOverview()
  const ask21 = page.find('[data-aircraft="a1"]')
  expect(ask21.text()).toContain('D-1234')
  expect(ask21.find('[data-kind="START"]').text()).toContain('Start of operations')
  expect(ask21.find('[data-kind="START"]').text()).toContain('In progress')
  expect(ask21.find('[data-kind="START"]').text()).toContain('1 of 2 checked')
  expect(ask21.find('[data-kind="START"]').text()).toContain('Recorder: Albers, Anna')
  expect(ask21.find('[data-kind="END"]').text()).toContain('Not started today')
  expect(ask21.find('[data-kind="END"]').text()).toContain('1 item')
  expect(ask21.find('[data-kind="START"] [data-open]').attributes('href')).toBe('/operations/checklist/a1/start')

  const ls8 = page.find('[data-aircraft="a2"]')
  expect(ls8.text()).toContain('No checklist set up')
  expect(ls8.find('[data-open]').exists()).toBe(false)
})

it ('Lets members only fill in checklists, not maintain them', async () => {
  const page = await mountOverview()
  expect(page.find('[data-edit-template]').exists()).toBe(false)
  expect(page.find('[data-create-template]').exists()).toBe(false)
  expect(page.find('[data-print]').exists()).toBe(false)
  expect(page.text()).not.toContain('Checklist archive')
})

it.each(['testmaintenanceadmin', 'testoperationsadmin', 'testadmin'])('Lets %s maintain the checklists and print their QR codes', async (username) => {
  const page = await mountOverview(username)
  expect(page.find('[data-aircraft="a1"] [data-edit-template]').exists()).toBe(true)
  expect(page.find('[data-aircraft="a1"] [data-print]').attributes('href')).toBe('/operations/print/a1/start')
  expect(page.findAll('[data-aircraft="a2"] [data-create-template]')).toHaveLength(2)
})

it ('Links the archive for operations admins only', async () => {
  expect((await mountOverview('testoperationsadmin')).text()).toContain('Checklist archive')
  expect((await mountOverview('testmaintenanceadmin')).text()).not.toContain('Checklist archive')
})

it ('Creates a checklist with one item per line', async () => {
  const page = await mountOverview('testoperationsadmin')
  await page.find('[data-aircraft="a2"] [data-kind="END"] [data-create-template]').trigger('click')
  await vi.waitFor(() => expect(document.body.querySelector('[data-template-items]')).not.toBeNull())
  expect(document.body.textContent).toContain('Create End of operations checklist for D-5678')

  const textarea = document.body.querySelector<HTMLTextAreaElement>('[data-template-items]')!
  textarea.value = 'Abdeckung drauf\n\n  Batterie laden  '
  textarea.dispatchEvent(new Event('input'))
  await vi.waitFor(() => expect(document.body.querySelector<HTMLButtonElement>('button[form="checklist-template-form"]')!.disabled).toBe(false))
  document.body.querySelector<HTMLButtonElement>('button[form="checklist-template-form"]')!.click()

  const calls = (globalThis as any).__apiCalls as { url: string, options: any }[]
  await vi.waitFor(() => expect(calls.some(call => call.url === '/operations/aircraft/a2/templates/END')).toBe(true))
  const saved = calls.filter(call => call.url === '/operations/aircraft/a2/templates/END').at(-1)!
  expect(saved.options.method).toBe('PUT')
  expect(saved.options.body).toEqual({ items: ['Abdeckung drauf', 'Batterie laden'] })
})
