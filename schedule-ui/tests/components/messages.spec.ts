import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import Messages from '~/components/messages.vue'

describe('Messages Component', () => {
    it('shows no alerts when both messages are null', async () => {
        const wrapper = await mountSuspended(Messages, {
            props: { errorMessage: null, successMessage: null }
        })
        expect(wrapper.html()).not.toContain('i-lucide-x')
        expect(wrapper.html()).not.toContain('i-lucide-check')
    })

    it('shows error message when errorMessage is set', async () => {
        const wrapper = await mountSuspended(Messages, {
            props: { errorMessage: 'Something went wrong', successMessage: null }
        })
        expect(wrapper.html()).toContain('Something went wrong')
    })

    it('shows success message when successMessage is set', async () => {
        const wrapper = await mountSuspended(Messages, {
            props: { errorMessage: null, successMessage: 'Saved successfully' }
        })
        expect(wrapper.html()).toContain('Saved successfully')
    })

    it('can show both error and success messages simultaneously', async () => {
        const wrapper = await mountSuspended(Messages, {
            props: { errorMessage: 'An error occurred', successMessage: 'Also a success' }
        })
        expect(wrapper.html()).toContain('An error occurred')
        expect(wrapper.html()).toContain('Also a success')
    })
})
