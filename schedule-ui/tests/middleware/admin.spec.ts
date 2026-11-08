import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockNavigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => mockNavigateTo)

describe('admin middleware', () => {
    let userStore: ReturnType<typeof useUserStore>

    beforeEach(() => {
        userStore = useUserStore()
    })

    beforeEach(async () => {
        mockNavigateTo.mockClear()
        await userStore.logout()
    })

    it('redirects user with ROLE_USER to /permission-denied', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        const { default: middleware } = await import('~/middleware/admin')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).toHaveBeenCalledWith('/permission-denied')
    })

    it('allows user with ROLE_ADMIN through', async () => {
        await userStore.login({ username: 'testadmin', password: 'password' })
        const { default: middleware } = await import('~/middleware/admin')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).not.toHaveBeenCalledWith('/permission-denied')
    })

    it('redirects ROLE_SCHEDULE_ADMIN (not a full admin) to /permission-denied', async () => {
        await userStore.login({ username: 'testscheduleadmin', password: 'password' })
        const { default: middleware } = await import('~/middleware/admin')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).toHaveBeenCalledWith('/permission-denied')
    })
})
