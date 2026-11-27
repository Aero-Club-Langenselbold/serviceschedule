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

    it('marks the rights of administrators as included and keeps them unchangeable', async () => {
        const wrapper = await mountPage()
        expect(wrapper.html()).toContain('Administrators hold every right')

        const adminRow = wrapper.findAll('tr').find(row => row.text().includes('testadmin'))!
        const checkboxes = adminRow.findAll('button[data-permission]')
        expect(checkboxes).toHaveLength(4)
        checkboxes.forEach(checkbox => expect(checkbox.attributes('disabled')).toBeDefined())
    })

    it('grants a further right on top of the existing one', async () => {
        const wrapper = await mountPage()
        const annaRow = wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        const [scheduleRight, alarmRight] = annaRow.findAll('button[data-permission]')
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
        const alarmRight = annaRow.findAll('button[data-permission]')[1]!

        await alarmRight.trigger('click')
        await flushPromises()

        expect(alarmRight.attributes('data-state')).toBe('unchecked')
    })

    it('offers only the administrator role as checkbox and keeps the base role as a badge', async () => {
        const wrapper = await mountPage()
        const annaRow = wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        expect(annaRow.text()).toContain('User')
        const roles = annaRow.findAll('button[data-role]')
        expect(roles).toHaveLength(1)
        expect(roles[0]!.attributes('data-role')).toBe('ROLE_ADMIN')
        expect(roles[0]!.attributes('data-state')).toBe('unchecked')
    })

    it('offers the operations admin right', async () => {
        const wrapper = await mountPage()
        const annaRow = wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        const operations = annaRow.find('button[data-permission="PERMISSION_OPERATIONS_MANAGE"]')
        expect(operations.exists()).toBe(true)
        expect(annaRow.text()).toContain('Operations admin')
        expect(operations.attributes('disabled')).toBeUndefined()
    })

    it('makes a user an administrator, which includes every right', async () => {
        const wrapper = await mountPage()
        const annaRow = () => wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        const adminRole = annaRow().find('button[data-role="ROLE_ADMIN"]')

        await adminRole.trigger('click')
        await flushPromises()

        expect(annaRow().find('button[data-role="ROLE_ADMIN"]').attributes('data-state')).toBe('checked')
        annaRow().findAll('button[data-permission]').forEach(right => expect(right.attributes('disabled')).toBeDefined())
        expect(useUserStore().user?.username).toBe('testadmin')
    })

    it('takes the administrator role away from another user', async () => {
        const wrapper = await mountPage()
        const annaRow = () => wrapper.findAll('tr').find(row => row.text().includes('anna@example.org'))!
        await annaRow().find('button[data-role="ROLE_ADMIN"]').trigger('click')
        await flushPromises()

        await annaRow().find('button[data-role="ROLE_ADMIN"]').trigger('click')
        await flushPromises()

        expect(annaRow().find('button[data-role="ROLE_ADMIN"]').attributes('data-state')).toBe('unchecked')
    })

    it('does not let administrators revoke their own administrator role', async () => {
        const wrapper = await mountPage()
        const adminRow = wrapper.findAll('tr').find(row => row.text().includes('testadmin'))!
        const ownAdmin = adminRow.find('button[data-role="ROLE_ADMIN"]')
        expect(ownAdmin.attributes('data-state')).toBe('checked')
        expect(ownAdmin.attributes('disabled')).toBeDefined()
        expect(adminRow.text()).toContain('You cannot revoke your own administrator role')
        // administrators hold every right anyway
        adminRow.findAll('button[data-permission]').forEach(right => expect(right.attributes('data-state')).toBe('checked'))
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
