import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import ChecklistView from '~/components/operations/checklist-view.vue'
import type { ChecklistDetail } from '~/types/operations'

const calls = () => (globalThis as any).__apiCalls as { url: string, options: any }[]

const detail = (overrides: Partial<ChecklistDetail> = {}, resultOfSecond: 'OK' | 'NOT_OK' | null = null): ChecklistDetail => ({
  checklist: {
    id: 'c1', aircraftId: 'a1', registration: 'D-1234', aircraftType: 'ASK 21', kind: 'START', date: '2026-11-27', status: 'IN_PROGRESS',
    createdBy: { id: 'u1', name: 'Albers, Anna' }, createdAt: '2026-11-27T07:55:00Z', submittedBy: null, submittedAt: null, approvedBy: null, approvedAt: null,
    itemCount: 2, checkedCount: resultOfSecond ? 2 : 1, notOkCount: resultOfSecond === 'NOT_OK' ? 1 : 0
  },
  items: [
    { id: 'ci1', position: 0, text: 'Haube reinigen', result: 'OK', comment: null, checkedBy: { id: 'u1', name: 'Albers, Anna' }, checkedAt: '2026-11-27T08:00:00Z', defect: null },
    { id: 'ci2', position: 1, text: 'Reifendruck prüfen', result: resultOfSecond, comment: null, checkedBy: null, checkedAt: null, defect: null }
  ],
  editable: true,
  submittable: !!resultOfSecond,
  approvable: false,
  reopenable: false,
  ...overrides
})

const mountView = async (value: ChecklistDetail) => {
  const wrapper = await mountSuspended(ChecklistView, { props: { detail: value } })
  await flushPromises()
  return wrapper
}

beforeEach(async () => {
  await useUserStore().login({ username: 'testuser', password: 'howwouldiknow' })
})

it ('Shows the initial recorder, the progress and who checked what', async () => {
  const wrapper = await mountView(detail())
  expect(wrapper.find('[data-created-by]').text()).toContain('Recorder:')
  expect(wrapper.find('[data-created-by]').text()).toContain('Albers, Anna')
  expect(wrapper.text()).toContain('1 of 2 checked')
  expect(wrapper.find('[data-item="0"]').text()).toContain('Checked by Albers, Anna')
  expect(wrapper.find('[data-item="0"] [data-result="OK"]').attributes('aria-pressed')).toBe('true')
})

it ('Checks an item as OK or not OK and emits the answer of the server', async () => {
  const wrapper = await mountView(detail())
  await wrapper.find('[data-item="1"] [data-result="NOT_OK"]').trigger('click')
  await flushPromises()

  const call = calls().filter(entry => entry.url === '/operations/checklists/c1/items/ci2').at(-1)!
  expect(call.options).toMatchObject({ method: 'PUT', body: { result: 'NOT_OK' } })
  const updated = wrapper.emitted('update')!.at(-1)![0] as ChecklistDetail
  expect(updated.items[1]!.result).toBe('NOT_OK')
})

it ('A second click on the chosen result unchecks the item', async () => {
  const wrapper = await mountView(detail())
  await wrapper.find('[data-item="0"] [data-result="OK"]').trigger('click')
  await flushPromises()
  expect(calls().filter(entry => entry.url === '/operations/checklists/c1/items/ci1').at(-1)!.options.body.result).toBeNull()
})

it ('Saves a remark when leaving the field', async () => {
  const wrapper = await mountView(detail())
  const input = wrapper.find('[data-item="1"] input[data-comment]')
  await input.setValue('nur 1,8 bar')
  await input.trigger('blur')
  await flushPromises()
  expect(calls().filter(entry => entry.url === '/operations/checklists/c1/items/ci2').at(-1)!.options.body).toEqual({ result: null, comment: 'nur 1,8 bar' })
})

it ('Reports a defect to the defect book from an item', async () => {
  const wrapper = await mountView(detail())
  await wrapper.find('[data-item="1"] [data-report-defect]').trigger('click')
  await vi.waitFor(() => expect(document.body.querySelector('[data-defect-title]')).not.toBeNull())
  expect(document.body.textContent).toContain('Reifendruck prüfen')

  const title = document.body.querySelector<HTMLInputElement>('[data-defect-title]')!
  title.value = 'Ventil undicht'
  title.dispatchEvent(new Event('input'))
  await vi.waitFor(() => expect(document.body.querySelector<HTMLButtonElement>('button[form="checklist-defect-form"]')!.disabled).toBe(false))
  document.body.querySelector<HTMLButtonElement>('button[form="checklist-defect-form"]')!.click()

  await vi.waitFor(() => expect(wrapper.emitted('update')).toBeTruthy())
  const call = calls().filter(entry => entry.url === '/operations/checklists/c1/items/ci2/defect').at(-1)!
  expect(call.options).toMatchObject({ method: 'POST', body: { title: 'Ventil undicht', priority: 'NORMAL', assigneeIds: null } })
  const updated = wrapper.emitted('update')!.at(-1)![0] as ChecklistDetail
  expect(updated.items[1]!.defect?.title).toBe('Ventil undicht')
})

it ('Links a reported defect and keeps its item not OK', async () => {
  const value = detail({}, 'NOT_OK')
  value.items[1]!.defect = { id: 'd9', title: 'Ventil undicht', status: 'OPEN' }
  const wrapper = await mountView(value)
  expect(wrapper.find('[data-item="1"] [data-defect-link]').attributes('href')).toBe('/maintenance/defects?defect=d9')
  expect(wrapper.find('[data-item="1"] [data-result="OK"]').attributes('disabled')).toBeDefined()
  expect(wrapper.find('[data-item="1"] [data-report-defect]').exists()).toBe(false)
})

it ('Confirms the checklist only once every item is checked', async () => {
  const incomplete = await mountView(detail())
  expect(incomplete.find('[data-submit]').attributes('disabled')).toBeDefined()
  expect(incomplete.text()).toContain('once every item is checked')

  const complete = await mountView(detail({}, 'OK'))
  await complete.find('[data-submit]').trigger('click')
  await flushPromises()
  expect(calls().at(-1)).toMatchObject({ url: '/operations/checklists/c1/submit', options: { method: 'POST' } })
  expect((complete.emitted('update')!.at(-1)![0] as ChecklistDetail).checklist.status).toBe('SUBMITTED')
})

it ('Shows a submitted checklist read only', async () => {
  const wrapper = await mountView(detail({ editable: false, submittable: false }, 'OK'))
  expect(wrapper.find('input[data-comment]').exists()).toBe(false)
  expect(wrapper.find('[data-submit]').exists()).toBe(false)
  wrapper.findAll('[data-result]').forEach(button => expect(button.attributes('disabled')).toBeDefined())
})
