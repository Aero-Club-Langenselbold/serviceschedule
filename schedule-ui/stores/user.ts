import { defineStore } from 'pinia'

interface User {
    username: string
    id: string
    roles: string[]
}

interface Session {
    token: string
    type: string
}

export const useUserStore = defineStore('user', () => {
    const user = ref<User | null>(null)
    const session = ref<Session | null>(null)

    const isLoggedIn = computed(() => session !== null && session?.value?.token !== undefined)

    async function login(credentials: { username: string | number, password: string | number}) {
        const response = await useAPI('/auth/signin', { method: 'POST', body: credentials })

        if (response.status.value !== "success") {
            throw new Error('Invalid credentials')
        }

        // store data
        user.value = {
            username: (response.data.value as User).username,
            id: (response.data.value as User).id,
            roles: (response.data.value as User).roles
        }
        session.value = {
            token: (response.data.value as Session).token,
            type: (response.data.value as Session).type
        }

        if (import.meta.client) {
            localStorage.setItem('user', JSON.stringify(user.value))
            localStorage.setItem('session', JSON.stringify(session.value))
        }
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

    return { user, session, isLoggedIn, login, logout, validate }
})