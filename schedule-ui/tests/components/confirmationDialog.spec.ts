import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import ConfirmationDialog from '~/components/confirmationDialog.vue'

// UTooltip requires a TooltipProvider ancestor — stub it out in isolation tests
// UModal teleports content outside the component tree and only renders when open — stub it to render slots directly
const globalStubs = {
    UTooltip: { template: '<slot />' },
    UModal: {
        props: ['title', 'open'],
        template: '<div data-modal><span>{{ title }}</span><slot /><slot name="body" /></div>'
    }
}

const defaultProps = {
    buttonLabel: 'Delete',
    modalTitle: 'Confirm Deletion',
    confirmLabel: 'Yes, delete',
    abortLabel: 'Cancel',
    tooltip: 'Delete this item',
    color: 'error' as const,
    variant: 'solid' as const,
    icon: 'i-lucide-trash',
    loading: false,
    buttonClass: '',
    onConfirm: () => {}
}

describe('ConfirmationDialog Component', () => {
    it('renders the trigger button', async () => {
        const wrapper = await mountSuspended(ConfirmationDialog, {
            props: defaultProps,
            global: { stubs: globalStubs }
        })
        expect(wrapper.html()).toContain('Delete')
    })

    it('renders the modal title', async () => {
        const wrapper = await mountSuspended(ConfirmationDialog, {
            props: defaultProps,
            global: { stubs: globalStubs }
        })
        expect(wrapper.html()).toContain('Confirm Deletion')
    })

    it('renders the confirm and abort labels', async () => {
        const wrapper = await mountSuspended(ConfirmationDialog, {
            props: defaultProps,
            global: { stubs: globalStubs }
        })
        expect(wrapper.html()).toContain('Yes, delete')
        expect(wrapper.html()).toContain('Cancel')
    })

    it('calls onConfirm when confirm button is clicked', async () => {
        let confirmed = false
        const wrapper = await mountSuspended(ConfirmationDialog, {
            props: {
                ...defaultProps,
                onConfirm: () => { confirmed = true }
            },
            global: { stubs: globalStubs }
        })
        const vm = wrapper.vm as any
        vm.handleConfirm()
        expect(confirmed).toBe(true)
    })
})
