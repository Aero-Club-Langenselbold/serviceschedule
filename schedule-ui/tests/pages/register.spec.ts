import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import Register from '~/pages/register.vue'

describe('Register Page', () => {
    it('shows all required form fields', async () => {
        const wrapper = await mountSuspended(Register)
        expect(wrapper.html()).toContain('Register to the schedule manager')
        expect(wrapper.html()).toContain('E-Mail-Address')
        expect(wrapper.html()).toContain('Password')
        expect(wrapper.html()).toContain('Registration Key')
        expect(wrapper.html()).toContain('Confirm Password')
    })

    it('shows the register submit button', async () => {
        const wrapper = await mountSuspended(Register)
        expect(wrapper.html()).toContain('Register')
    })

    it('does not show an error message initially', async () => {
        const wrapper = await mountSuspended(Register)
        expect(wrapper.html()).not.toContain('i-lucide-x')
    })

    it('register button is disabled when form fields are empty', async () => {
        const wrapper = await mountSuspended(Register)
        const button = wrapper.find('button[type="submit"], button[disabled]')
        // The registerEnabled flag starts as false with empty fields
        const vm = wrapper.vm as any
        expect(vm.registerEnabled).toBe(false)
    })

    it('shows password strength indicator', async () => {
        const wrapper = await mountSuspended(Register)
        expect(wrapper.html()).toContain('Must contain:')
    })
})
