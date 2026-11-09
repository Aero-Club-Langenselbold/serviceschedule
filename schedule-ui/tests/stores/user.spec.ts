import { beforeEach, describe, expect, it } from 'vitest'

describe('User Store', () => {
    let userStore: ReturnType<typeof useUserStore>

    beforeEach(() => {
        userStore = useUserStore()
    })

    beforeEach(async () => {
        await userStore.logout()
    })

    it('starts with no user and no session', () => {
        expect(userStore.user).toBeNull()
        expect(userStore.session).toBeNull()
    })

    it('isLoggedIn is false when not authenticated', () => {
        expect(userStore.isLoggedIn).toBe(false)
    })

    it('login sets user data', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        expect(userStore.user).not.toBeNull()
        expect(userStore.user?.username).toBe('testuser')
        expect(userStore.user?.id).toBe('1')
    })

    it('login sets session token', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        expect(userStore.session?.token).toBe('testtoken')
        expect(userStore.session?.type).toBe('Bearer')
    })

    it('isLoggedIn is true after successful login', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        expect(userStore.isLoggedIn).toBe(true)
    })

    it('regular user has ROLE_USER', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        expect(userStore.user?.roles).toContain('ROLE_USER')
        expect(userStore.user?.roles).not.toContain('ROLE_ADMIN')
    })

    it('admin user has ROLE_ADMIN', async () => {
        await userStore.login({ username: 'testadmin', password: 'password' })
        expect(userStore.user?.roles).toContain('ROLE_ADMIN')
    })

    it('schedule admin user has ROLE_SCHEDULE_ADMIN', async () => {
        await userStore.login({ username: 'testscheduleadmin', password: 'password' })
        expect(userStore.user?.roles).toContain('ROLE_SCHEDULE_ADMIN')
    })

    it('logout clears user state', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        await userStore.logout()
        expect(userStore.user).toBeNull()
        expect(userStore.session).toBeNull()
    })

    it('isLoggedIn is false after logout', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        await userStore.logout()
        expect(userStore.isLoggedIn).toBe(false)
    })

    it('stores the rights of a user', async () => {
        await userStore.login({ username: 'testdelegate', password: 'password' })
        expect(userStore.user?.permissions).toEqual(['PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE'])
        expect(userStore.hasPermission('PERMISSION_ALARM_MANAGE')).toBe(true)
    })

    it('rights unlock a module without a matching role', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        expect(userStore.canManageSchedule).toBe(false)
        expect(userStore.canManageAlarm).toBe(false)

        await userStore.login({ username: 'testdelegate', password: 'password' })
        expect(userStore.canManageSchedule).toBe(true)
        expect(userStore.canManageAlarm).toBe(true)
    })

    it('roles keep unlocking their module', async () => {
        await userStore.login({ username: 'testscheduleadmin', password: 'password' })
        expect(userStore.canManageSchedule).toBe(true)
        expect(userStore.canManageAlarm).toBe(false)

        await userStore.login({ username: 'testadmin', password: 'password' })
        expect(userStore.canManageSchedule).toBe(true)
        expect(userStore.canManageAlarm).toBe(true)
    })

    it('handles sessions stored before rights existed', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        userStore.user = { id: '1', username: 'testuser', roles: ['ROLE_USER'] }
        expect(userStore.hasPermission('PERMISSION_ALARM_MANAGE')).toBe(false)
        expect(userStore.canManageAlarm).toBe(false)
    })

    it('impersonate replaces the session and survives a reload', async () => {
        await userStore.login({ username: 'testadmin', password: 'password' })
        expect(userStore.isImpersonating).toBe(false)

        await userStore.impersonate('u1')
        expect(userStore.isImpersonating).toBe(true)
        expect(userStore.user?.username).toBe('impersonated')
        expect(userStore.user?.impersonator).toBe('testadmin')
        expect(userStore.user?.roles).not.toContain('ROLE_ADMIN')
        expect(userStore.session?.token).toBe('impersonationtoken')

        userStore.user = null
        await userStore.validate()
        expect(userStore.isImpersonating).toBe(true)
    })

    it('stopImpersonation returns to the administrator', async () => {
        await userStore.login({ username: 'testadmin', password: 'password' })
        await userStore.impersonate('u1')

        await userStore.stopImpersonation()
        expect(userStore.isImpersonating).toBe(false)
        expect(userStore.user?.username).toBe('testadmin')
        expect(userStore.user?.roles).toContain('ROLE_ADMIN')
        expect(userStore.session?.token).toBe('admintoken')
    })

    it('a failed impersonation keeps the current session', async () => {
        await userStore.login({ username: 'testadmin', password: 'password' })

        await expect(userStore.impersonate('failing')).rejects.toThrow()
        expect(userStore.user?.username).toBe('testadmin')
        expect(userStore.session?.token).toBe('testtoken')
    })

    it('validate restores user from localStorage', async () => {
        await userStore.login({ username: 'testuser', password: 'password' })
        // Simulate a new store instance by manually clearing in-memory state
        userStore.user = null
        userStore.session = null

        await userStore.validate()
        expect(userStore.user?.username).toBe('testuser')
        expect(userStore.session?.token).toBe('testtoken')
    })
})
