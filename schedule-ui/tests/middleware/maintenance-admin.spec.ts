import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockNavigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => mockNavigateTo)

describe('maintenance-admin middleware', () => {
    let userStore: ReturnType<typeof useUserStore>

    beforeEach(async () => {
        userStore = useUserStore()
        mockNavigateTo.mockClear()
        await userStore.logout()
    })

    it.each([
                ['testadmin', 'ROLE_ADMIN'],
        ['testmaintenancedelegate', 'PERMISSION_MAINTENANCE_MANAGE'],
    ])('allows %s (%s) through', async (username) => {
        await userStore.login({ username, password: 'password' })
        const { default: middleware } = await import('~/middleware/maintenance-admin')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).not.toHaveBeenCalled()
    })

    it.each([
        ['testuser', 'ROLE_USER'],
        ['testoperationsadmin', 'PERMISSION_OPERATIONS_MANAGE'],
        ['testscheduleadmin', 'PERMISSION_SCHEDULE_MANAGE'],
        ['testdelegate', 'other rights only'],
    ])('redirects %s (%s) to /permission-denied', async (username) => {
        await userStore.login({ username, password: 'password' })
        const { default: middleware } = await import('~/middleware/maintenance-admin')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).toHaveBeenCalledWith('/permission-denied')
    })
})
