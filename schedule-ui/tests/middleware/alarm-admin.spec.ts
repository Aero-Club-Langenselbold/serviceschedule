import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockNavigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => mockNavigateTo)

describe('alarm-admin middleware', () => {
    let userStore: ReturnType<typeof useUserStore>

    beforeEach(async () => {
        userStore = useUserStore()
        mockNavigateTo.mockClear()
        await userStore.logout()
    })

    it.each([
        ['testalarmadmin', 'ROLE_ALARM_ADMIN'],
        ['testadmin', 'ROLE_ADMIN'],
    ])('allows %s (%s) through', async (username) => {
        await userStore.login({ username, password: 'password' })
        const { default: middleware } = await import('~/middleware/alarm-admin')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).not.toHaveBeenCalled()
    })

    it.each([
        ['testuser', 'ROLE_USER'],
        ['testscheduleadmin', 'ROLE_SCHEDULE_ADMIN'],
    ])('redirects %s (%s) to /permission-denied', async (username) => {
        await userStore.login({ username, password: 'password' })
        const { default: middleware } = await import('~/middleware/alarm-admin')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).toHaveBeenCalledWith('/permission-denied')
    })
})
