import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UsersList from '~/pages/users/list.vue'

const mockNavigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => mockNavigateTo)

describe('Users Page', () => {
    const mountPage = async () => {
        const wrapper = await mountSuspended(UsersList)
        await flushPromises()
        return wrapper
    }

    beforeEach(async () => {
        mockNavigateTo.mockClear()
        await useUserStore().login({ username: 'testadmin', password: 'password' })
    })

    it('lists the basic data of registered users', async () => {
        const wrapper = await mountPage()
        expect(wrapper.html()).toContain('anna@example.org')
        expect(wrapper.html()).toContain('Albers, Anna')
        expect(wrapper.html()).toContain('No participant')
    })

    it('shows roles and rights', async () => {
        const wrapper = await mountPage()
        expect(wrapper.html()).toContain('Administrator')
        expect(wrapper.html()).toContain('Manage service plans')
        expect(wrapper.html()).toContain('Manage alarm system')
    })

    it('marks rights already included in a role and keeps them unchangeable', async () => {
        const wrapper = await mountPage()
        expect(wrapper.html()).toContain('Already included in the role')

        const adminRow = wrapper.findAll('tr').find(row => row.text().includes('testadmin'))!
        const checkboxes = adminRow.findAll('button[role="checkbox"]')
        expect(checkboxes).toHaveLength(2)
        checkboxes.forEach(checkbox => expect(checkbox.attributes('disabled')).toBeDefined())
    })

    it('grants a further right on top of the existing one', async () => {
        const wrapper = await mountPage()
        const annaRow = wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        const [scheduleRight, alarmRight] = annaRow.findAll('button[role="checkbox"]')
        expect(scheduleRight!.attributes('data-state')).toBe('unchecked')
        expect(alarmRight!.attributes('data-state')).toBe('checked')

        await scheduleRight!.trigger('click')
        await flushPromises()

        expect(scheduleRight!.attributes('data-state')).toBe('checked')
        expect(alarmRight!.attributes('data-state')).toBe('checked')
    })

    it('revokes a right', async () => {
        const wrapper = await mountPage()
        const annaRow = wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        const alarmRight = annaRow.findAll('button[role="checkbox"]')[1]!

        await alarmRight.trigger('click')
        await flushPromises()

        expect(alarmRight.attributes('data-state')).toBe('unchecked')
    })

    it('does not offer to impersonate administrators', async () => {
        const wrapper = await mountPage()
        const adminRow = wrapper.findAll('tr').find(row => row.text().includes('testadmin'))!
        const button = adminRow.findAll('button').find(candidate => candidate.text().includes('Impersonate'))!
        expect(button.attributes('disabled')).toBeDefined()
    })

    it('impersonates a user and opens the start page', async () => {
        const wrapper = await mountPage()
        const annaRow = wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        const button = annaRow.findAll('button').find(candidate => candidate.text().includes('Impersonate'))!

        await button.trigger('click')
        await flushPromises()

        expect(useUserStore().isImpersonating).toBe(true)
        expect(mockNavigateTo).toHaveBeenCalledWith('/')
    })
})
