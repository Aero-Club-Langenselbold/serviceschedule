import { beforeEach, describe, expect, it } from 'vitest'

describe('User Store', () => {
    const userStore = useUserStore()

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
