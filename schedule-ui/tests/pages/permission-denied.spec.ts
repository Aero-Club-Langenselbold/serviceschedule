import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it } from 'vitest'
import PermissionDenied from '~/pages/permission-denied.vue'

describe('Permission Denied Page', () => {
    beforeEach(async () => {
        // Must be logged in for the authenticated middleware to pass
        await useUserStore().login({ username: 'testuser', password: 'password' })
    })

    it('renders the permission denied alert', async () => {
        const wrapper = await mountSuspended(PermissionDenied)
        expect(wrapper.html()).toContain('Permission Denied')
    })

    it('shows a descriptive error message', async () => {
        const wrapper = await mountSuspended(PermissionDenied)
        expect(wrapper.html()).toContain('You do not have permission to access this page.')
    })
})
