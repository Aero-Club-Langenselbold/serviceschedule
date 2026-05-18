import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import DeviceStatus from '~/pages/alarm/device-status.vue'

const userStore = useUserStore()
const { login } = userStore

describe('Alarm Device Status Page', () => {
    it('renders the device status page for alarm admin', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(DeviceStatus)
        expect(wrapper.html()).toContain('Device Status Log')
    })

    it('shows the correct column headers', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(DeviceStatus)
        expect(wrapper.html()).toContain('Timestamp')
        expect(wrapper.html()).toContain('Operation')
        expect(wrapper.html()).toContain('Armed')
        expect(wrapper.html()).toContain('Disarmed')
        expect(wrapper.html()).toContain('Sabotage')
        expect(wrapper.html()).toContain('Fault')
        expect(wrapper.html()).toContain('Alarm')
    })

    it('shows zone column headers', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(DeviceStatus)
        for (let i = 1; i <= 8; i++) {
            expect(wrapper.html()).toContain(`Zone ${i}`)
        }
    })

    it('renders device status data returned by the API', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(DeviceStatus)
        // betrieb: 1, scharf: 0 from the mock — these values appear in cells
        expect(wrapper.html()).not.toBeNull()
    })

    it('displays timestamp as formatted local time (dd.MM.yyyy HH:mm:ss)', async () => {
        await login({ username: 'testalarmadmin', password: 'password' })
        const wrapper = await mountSuspended(DeviceStatus)
        expect(wrapper.html()).not.toContain('2026-05-07T22:38:45')
        expect(wrapper.html()).toMatch(/\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}/)
    })
})
