import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import Spinner from '~/components/spinner.vue'

describe('Spinner Component', () => {
    it('renders the spinner SVG', async () => {
        const wrapper = await mountSuspended(Spinner)
        expect(wrapper.html()).toContain('svg')
    })

    it('applies animation class', async () => {
        const wrapper = await mountSuspended(Spinner)
        expect(wrapper.html()).toContain('animate-spin')
    })

    it('is centered in its container', async () => {
        const wrapper = await mountSuspended(Spinner)
        expect(wrapper.html()).toContain('flex')
        expect(wrapper.html()).toContain('justify-center')
        expect(wrapper.html()).toContain('items-center')
    })
})
