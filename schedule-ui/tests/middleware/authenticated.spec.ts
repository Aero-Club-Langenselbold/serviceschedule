import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockNavigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => mockNavigateTo)

describe('authenticated middleware', () => {
    const userStore = useUserStore()

    beforeEach(async () => {
        mockNavigateTo.mockClear()
        await userStore.logout()
    })

    it('redirects unauthenticated user to /login', async () => {
        const { default: middleware } = await import('~/middleware/authenticated')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).toHaveBeenCalledWith('/login')
    })

    it('does not redirect authenticated user', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        const { default: middleware } = await import('~/middleware/authenticated')
        await middleware({} as any, {} as any)
        expect(mockNavigateTo).not.toHaveBeenCalledWith('/login')
    })
})
