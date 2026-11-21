import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PlanUnfilledDialog from '~/components/plan-unfilled-dialog.vue'

describe('Unfilled services dialog', () => {
  const items = [
    { date: '2026-04-11', serviceId: 's1', servicename: 'Flugleiter', startTime: '09:00:00', endTime: '13:30:00', required: 1, assigned: 0 },
    { date: '2026-04-11', serviceId: 's2', servicename: 'Windenfahrer', startTime: '09:00:00', endTime: '18:00:00', required: 2, assigned: 1 },
    { date: '2026-04-18', serviceId: 's1', servicename: 'Flugleiter', startTime: '13:30:00', endTime: '18:00:00', required: 1, assigned: 0 }
  ]

  it('lists the unfilled services per day with a link to the plan', async () => {
    const wrapper = await mountSuspended(PlanUnfilledDialog, { props: { open: true, planId: 'p1', planName: 'Saison 2026', items }, attachTo: document.body })
    await flushPromises()
    const text = document.body.textContent ?? ''

    expect(text).toContain('Not enough people were found for 3 services')
    expect(text).toContain('Saison 2026')
    expect(document.body.querySelectorAll('h3')).toHaveLength(2)
    expect(text).toContain('09:00 – 13:30')
    expect(text).toContain('Nobody assigned')
    expect(text).toContain('1 of 2 assigned')
    expect(document.body.querySelector('a[href="/plan/generated/p1"]')?.textContent).toContain('Edit service plan')
    wrapper.unmount()
  })
})
