import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import AlarmList from '~/pages/alarm/list.vue'

const userStore = useUserStore()
const { login } = userStore

describe('Alarm List Page', () => {
    it('renders the alarm list page for alarm admin', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(AlarmList)
        expect(wrapper.html()).toContain('Event Log')
    })

    it('shows the correct column headers', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(AlarmList)
        expect(wrapper.html()).toContain('Timestamp')
        expect(wrapper.html()).toContain('RFID ID')
        expect(wrapper.html()).toContain('Internal Time')
        expect(wrapper.html()).toContain('Status')
        expect(wrapper.html()).toContain('Raw Data')
    })

    it('renders alarm data returned by the API', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(AlarmList)
        expect(wrapper.html()).toContain('AABBCC')
        expect(wrapper.html()).toContain('22:38:45')
    })

    it('displays timestamp as formatted local time (dd.MM.yyyy HH:mm:ss)', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(AlarmList)
        // Timestamp should be formatted, not raw ISO string
        expect(wrapper.html()).not.toContain('2026-05-07T22:38:45')
        expect(wrapper.html()).toMatch(/\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}/)
    })
})
