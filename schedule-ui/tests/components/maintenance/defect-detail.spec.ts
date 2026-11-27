import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import DefectDetail from '~/components/maintenance/defect-detail.vue'

const stubs = { ConfirmationDialog: true }

const mountDetail = async (defectId: string, username = 'testuser') => {
  await useUserStore().login({ username, password: 'howwouldiknow' })
  const wrapper = await mountSuspended(DefectDetail, { props: { defectId, open: true }, global: { stubs } })
  await vi.waitFor(() => expect(document.body.querySelector('[data-defect-status]')).not.toBeNull())
  return wrapper
}
const body = () => document.body
const button = (label: string) => [...body().querySelectorAll('button')].find(candidate => candidate.textContent?.trim() === label) as HTMLButtonElement | undefined
const status = () => body().querySelector('[data-defect-status]')?.textContent?.trim()

it ('Lets every member work on an open defect, but not reject or sign it off', async () => {
  const wrapper = await mountDetail('d1')
  expect(button('Start working')).toBeDefined()
  expect(button('Report as done')).toBeDefined()
  expect(button('Reject')).toBeUndefined()
  expect(button('Sign off')).toBeUndefined()
  // assignment and comments in the activity
  expect(body().querySelector('[data-activity]')?.textContent).toContain('assigned testuser')

  button('Report as done')!.click()
  await flushPromises()
  await vi.waitFor(() => expect(status()).toBe('Done'))
  expect(wrapper.emitted('changed')).toBeTruthy()
  wrapper.unmount()
})

it ('Lets maintenance administrators reject a defect with a reason', async () => {
  const wrapper = await mountDetail('d1', 'testmaintenanceadmin')
  button('Reject')!.click()
  await vi.waitFor(() => expect(body().querySelector('[data-reject-form]')).not.toBeNull())
  const form = body().querySelector('[data-reject-form]')!
  const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement
  expect(submit.disabled).toBe(true)

  const reason = form.querySelector('textarea')!
  reason.value = 'Nur ein Schmutzfleck'
  reason.dispatchEvent(new Event('input'))
  await vi.waitFor(() => expect(submit.disabled).toBe(false))
  submit.click()

  await vi.waitFor(() => expect(status()).toBe('Rejected'))
  expect(body().querySelector('[data-event="REJECTED"]')?.textContent).toContain('Nur ein Schmutzfleck')
  // administrators take the rejection back
  expect(button('Reopen')).toBeDefined()
  wrapper.unmount()
})

it ('Lets only maintenance administrators sign off a done defect', async () => {
  const user = await mountDetail('d3')
  expect(button('Reopen')).toBeDefined()
  expect(button('Sign off')).toBeUndefined()
  user.unmount()

  const admin = await mountDetail('d3', 'testmaintenanceadmin')
  button('Sign off')!.click()
  await vi.waitFor(() => expect(status()).toBe('Signed off'))
  expect(body().textContent).toContain('Signed off defects are locked')
  expect(button('Withdraw sign-off')).toBeDefined()
  admin.unmount()
})

it ('Locks signed off defects for comments and work', async () => {
  const wrapper = await mountDetail('d4')
  expect(body().querySelector('textarea')).toBeNull()
  expect(button('Reopen')).toBeUndefined()
  expect(button('Withdraw sign-off')).toBeUndefined()
  wrapper.unmount()
})

it ('Adds a comment to the activity', async () => {
  const wrapper = await mountDetail('d2')
  const textarea = body().querySelector('textarea[aria-label="Comment"]') as HTMLTextAreaElement
  textarea.value = 'Neues Seil ist bestellt'
  textarea.dispatchEvent(new Event('input'))
  await flushPromises()
  button('Comment')!.click()

  await vi.waitFor(() => expect(body().querySelector('[data-activity]')?.textContent).toContain('Neues Seil ist bestellt'))
  wrapper.unmount()
})

it ('Closes when the defect does not exist anymore', async () => {
  await useUserStore().login({ username: 'testuser', password: 'howwouldiknow' })
  const wrapper = await mountSuspended(DefectDetail, { props: { defectId: 'missing', open: true }, global: { stubs } })
  await vi.waitFor(() => expect(wrapper.emitted('update:open')?.at(-1)).toEqual([false]))
  wrapper.unmount()
})
