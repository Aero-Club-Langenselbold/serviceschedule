import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockNavigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => mockNavigateTo)

describe('kiosk middleware', () => {
    beforeEach(async () => {
        mockNavigateTo.mockClear()
        await useUserStore().logout()
    })

    const run = async (path: string) => {
        const { default: middleware } = await import('~/middleware/kiosk.global')
        return middleware({ path } as any, {} as any)
    }

    it('keeps kiosk sessions on maintenance and operations pages', async () => {
        await useUserStore().loginKiosk({ username: 'anna@example.org', pin: '123456' })
        for (const path of ['/maintenance', '/maintenance/a1', '/maintenance/defects', '/operations', '/operations/checklist/a1/start', '/kiosk']) {
            await run(path)
        }
        expect(mockNavigateTo).not.toHaveBeenCalled()
    })

    it('sends kiosk sessions from every other page to the kiosk start page', async () => {
        await useUserStore().loginKiosk({ username: 'anna@example.org', pin: '123456' })
        for (const path of ['/', '/request/list', '/profile', '/maintenance/templates', '/operations/archive', '/users/list']) {
            mockNavigateTo.mockClear()
            await run(path)
            expect(mockNavigateTo).toHaveBeenCalledWith('/kiosk')
        }
    })

    it('leaves regular sessions alone but keeps them off the kiosk start page', async () => {
        await useUserStore().login({ username: 'testadmin', password: 'password' })
        await run('/request/list')
        await run('/maintenance/templates')
        expect(mockNavigateTo).not.toHaveBeenCalled()

        await run('/kiosk')
        expect(mockNavigateTo).toHaveBeenCalledWith('/')
    })
})
