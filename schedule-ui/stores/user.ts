import { defineStore } from 'pinia'

interface User {
    username: string
    id: string
    roles: string[]
    permissions?: string[]
    impersonator?: string | null
}

interface Session {
    token: string
    type: string
}

export const useUserStore = defineStore('user', () => {
    const user = ref<User | null>(null)
    const session = ref<Session | null>(null)

    const isLoggedIn = computed(() => session !== null && session?.value?.token !== undefined)

    const isImpersonating = computed(() => !!user.value?.impersonator)

    const hasRole = (...roles: string[]) => roles.some(role => user.value?.roles?.includes(role))
    const hasPermission = (permission: string) => !!user.value?.permissions?.includes(permission)

    const canManageSchedule = computed(() => hasRole('ROLE_ADMIN', 'ROLE_SCHEDULE_ADMIN') || hasPermission('PERMISSION_SCHEDULE_MANAGE'))
    const canManageAlarm = computed(() => hasRole('ROLE_ADMIN', 'ROLE_ALARM_ADMIN') || hasPermission('PERMISSION_ALARM_MANAGE'))

    function storeAuthentication(data: User & Session) {
        user.value = {
            username: data.username,
            id: data.id,
            roles: data.roles,
            permissions: data.permissions ?? [],
            impersonator: data.impersonator ?? null
        }
        session.value = {
            token: data.token,
            type: data.type
        }

        if (import.meta.client) {
            localStorage.setItem('user', JSON.stringify(user.value))
            localStorage.setItem('session', JSON.stringify(session.value))
        }
    }

    async function login(credentials: { username: string | number, password: string | number}) {
        const response = await useAPI('/auth/signin', { method: 'POST', body: credentials })

        if (response.error?.value) {
            throw new Error('Invalid credentials')
        }

        storeAuthentication(response.data.value as User & Session)
    }

    async function impersonate(id: string) {
        const response = await useAPI(`/user/${id}/impersonate`, { method: 'POST' })

        if (response.error?.value) {
            throw new Error('Impersonation failed')
        }

        storeAuthentication(response.data.value as User & Session)
    }

    async function stopImpersonation() {
        const response = await useAPI('/user/impersonation/stop', { method: 'POST' })

        if (response.error?.value) {
            throw new Error('Could not leave impersonation')
        }

        storeAuthentication(response.data.value as User & Session)
    }

    async function validate() {
        if (import.meta.client) {
            const storedUser = localStorage.getItem('user')
            const storedSession = localStorage.getItem('session')
            if (storedUser) {
                user.value = JSON.parse(storedUser)
            }
            if (storedSession) {
                session.value = JSON.parse(storedSession)
            }
        }
    }

    async function logout() {
        // clear localstorage
        localStorage.removeItem('user')
        localStorage.removeItem('session')
        user.value = null
        session.value = null
    }

    return { user, session, isLoggedIn, isImpersonating, canManageSchedule, canManageAlarm, hasRole, hasPermission, login, impersonate, stopImpersonation, logout, validate }
})