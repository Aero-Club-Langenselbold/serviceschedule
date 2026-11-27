import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Login from '~/pages/login.vue'

const { mockNavigateTo } = vi.hoisted(() => ({ mockNavigateTo: vi.fn() }))
mockNuxtImport('navigateTo', () => mockNavigateTo)

const calls = () => (globalThis as any).__apiCalls as { url: string, options: any }[]

it ('Can render Login Page', async () => {
    const login = await mountSuspended(Login)
    expect(login.html()).toContain('Username')
    expect(login.html()).toContain('Password')
    expect(login.html()).toContain('Login')
    expect(login.html()).toContain('Register')
    expect(login.html()).toContain('Forgot password?')
    expect(login.html()).not.toContain('Invalid credentials')
})

describe('kiosk mode', () => {
    beforeEach(async () => {
        mockNavigateTo.mockClear()
        await useUserStore().logout()
    })

    afterEach(() => {
        localStorage.removeItem('loginMode')
        localStorage.removeItem('features')
        useFeatureStore().features = []
    })

    const typePin = async (page: Awaited<ReturnType<typeof mountSuspended>>, pin: string) => {
        const inputs = page.findAll('[data-kiosk-pin] input[data-slot="base"]')
        expect(inputs).toHaveLength(6)
        for (const [index, digit] of [...pin].entries()) {
            await inputs[index]!.setValue(digit)
        }
    }

    const chooseKiosk = async (page: Awaited<ReturnType<typeof mountSuspended>>) => {
        const tab = page.findAll('[data-login-mode] button').find(button => button.text() === 'Kiosk')!
        await tab.trigger('mousedown', { button: 0 })
        await tab.trigger('focus')
        await vi.waitFor(() => expect(page.find('[data-kiosk-login]').exists()).toBe(true))
    }

    it('offers regular and kiosk sign in, regular by default', async () => {
        const page = await mountSuspended(Login, { route: '/login' })
        expect(page.find('[data-login-mode]').text()).toContain('Regular')
        expect(page.find('[data-login-mode]').text()).toContain('Kiosk')
        expect(page.find('[data-kiosk-login]').exists()).toBe(false)
        expect(page.text()).toContain('Password')
    })

    it('replaces the password by the PIN and drops every other field, also with the Vereinsflieger login', async () => {
        // loaded by app.vue before the page opens
        useFeatureStore().features = [
            { id: 'vfLoginFeature', description: '', active: true },
            { id: 'securityQuestionFeature', description: '', active: true }
        ]
        const page = await mountSuspended(Login, { route: '/login' })
        // the regular form asks for the 2FA code
        expect(page.text()).toContain('Authentication Secret')

        await chooseKiosk(page)
        expect(page.find('[data-kiosk-username]').exists()).toBe(true)
        expect(page.findAll('[data-kiosk-pin] input[data-slot="base"]')).toHaveLength(6)
        expect(page.text()).not.toContain('Password')
        expect(page.text()).not.toContain('Authentication Secret')
        expect(page.text()).not.toContain('Sicherheitsfrage')
        expect(page.text()).not.toContain('Register')
        expect(page.text()).not.toContain('Forgot password?')
        // masked like a password
        page.findAll('[data-kiosk-pin] input[data-slot="base"]').forEach(input => expect(input.attributes('type')).toBe('password'))
    })

    it('remembers the choice on the device', async () => {
        const page = await mountSuspended(Login, { route: '/login' })
        await chooseKiosk(page)
        expect(localStorage.getItem('loginMode')).toBe('kiosk')

        const again = await mountSuspended(Login, { route: '/login' })
        expect(again.find('[data-kiosk-login]').exists()).toBe(true)
    })

    it('signs in with e-mail and PIN and opens the kiosk start page', async () => {
        localStorage.setItem('loginMode', 'kiosk')
        const page = await mountSuspended(Login, { route: '/login' })
        await page.find('input[data-kiosk-username]').setValue('anna@example.org')
        await typePin(page, '123456')
        await page.find('[data-kiosk-login]').trigger('submit')
        await flushPromises()

        expect(calls().filter(call => call.url === '/auth/kiosk/signin').at(-1)!.options.body).toEqual({ username: 'anna@example.org', pin: '123456' })
        await vi.waitFor(() => expect(mockNavigateTo).toHaveBeenCalledWith('/kiosk'))
        expect(useUserStore().isKiosk).toBe(true)
    })

    it('reports a wrong or missing PIN and clears it', async () => {
        localStorage.setItem('loginMode', 'kiosk')
        const page = await mountSuspended(Login, { route: '/login' })
        await page.find('input[data-kiosk-username]').setValue('anna@example.org')
        await typePin(page, '000000')
        await page.find('[data-kiosk-login]').trigger('submit')
        await flushPromises()

        await vi.waitFor(() => expect(page.text()).toContain('no kiosk PIN is set'))
        expect(useUserStore().isLoggedIn).toBe(false)
        expect(mockNavigateTo).not.toHaveBeenCalled()
        page.findAll('[data-kiosk-pin] input[data-slot="base"]').forEach(input => expect((input.element as HTMLInputElement).value).toBe(''))
    })

    it('reports a locked account', async () => {
        localStorage.setItem('loginMode', 'kiosk')
        const page = await mountSuspended(Login, { route: '/login' })
        await page.find('input[data-kiosk-username]').setValue('anna@example.org')
        await typePin(page, '999999')
        await page.find('[data-kiosk-login]').trigger('submit')
        await flushPromises()

        await vi.waitFor(() => expect(page.text()).toContain('locked for 15 minutes'))
    })
})
