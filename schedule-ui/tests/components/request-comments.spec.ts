import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RequestComments from '~/components/request-comments.vue'

describe('Request comments', () => {
    const mountComments = async (status: string, requestId = 'r1', canClose = false) => {
        const wrapper = await mountSuspended(RequestComments, { props: { requestId, status: status as any, canClose }, attachTo: document.body })
        await flushPromises()
        return wrapper
    }

    it('lists existing comments with their author', async () => {
        const wrapper = await mountComments('OPEN')
        expect(wrapper.html()).toContain('Tritt bei mir auch auf')
        expect(wrapper.html()).toContain('Becker, Ben')
    })

    it.each(['OPEN', 'IN_PROGRESS'])('offers the comment form for status %s', async (status) => {
        const wrapper = await mountComments(status)
        expect(wrapper.find('textarea').exists()).toBe(true)
    })

    it.each(['CLOSED', 'REJECTED'])('keeps comments readable but offers no form for status %s', async (status) => {
        const wrapper = await mountComments(status)
        expect(wrapper.html()).toContain('Tritt bei mir auch auf')
        expect(wrapper.find('textarea').exists()).toBe(false)
        expect(wrapper.html()).toContain('Only open entries can be commented on.')
    })

    it('does not submit blank comments', async () => {
        const wrapper = await mountComments('OPEN')
        await wrapper.find('textarea').setValue('   ')
        expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
    })

    it('appends a new comment and clears the form', async () => {
        const wrapper = await mountComments('OPEN')
        await wrapper.find('textarea').setValue('Neuer Hinweis')
        await wrapper.find('form').trigger('submit')
        await flushPromises()

        expect(wrapper.html()).toContain('Neuer Hinweis')
        expect(wrapper.html()).toContain('Tritt bei mir auch auf')
        expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('')
    })

    it('reports comments that cannot be loaded', async () => {
        const wrapper = await mountComments('OPEN', 'failing')
        expect(wrapper.html()).toContain('Could not load the comments')
    })

    it('offers closing and rejecting only to administrators', async () => {
        expect((await mountComments('OPEN')).find('[data-comment-actions]').exists()).toBe(false)
        expect((await mountComments('OPEN', 'r1', true)).find('[data-comment-actions]').exists()).toBe(true)
    })

    it.each([
        ['Comment and close', 'CLOSED'],
        ['Comment and reject', 'REJECTED']
    ])('lets administrators %s with one comment', async (label, status) => {
        const wrapper = await mountComments('OPEN', 'r1', true)
        await wrapper.find('textarea').setValue('Erledigt')
        await wrapper.find('[data-comment-actions]').trigger('click')
        await flushPromises()

        const item = [...document.body.querySelectorAll('[role="menuitem"]')].find(element => element.textContent?.includes(label)) as HTMLElement
        expect(item).toBeDefined()
        item.click()
        await flushPromises()

        expect(wrapper.html()).toContain('Erledigt')
        expect(wrapper.emitted('statusChanged')).toEqual([[status]])
        wrapper.unmount()
    })
})
