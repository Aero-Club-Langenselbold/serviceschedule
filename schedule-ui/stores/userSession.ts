import { defineStore } from 'pinia'

interface User {
  id: string
  username: string
  roles: string[]
}

interface Session {
  token: string
  type: string
}

export const useUserSessionStore = defineStore('userSessionStore', {
  state: () => {
    return {
      user: null as User | null,
      session: null as Session | null,
      loggedIn: false
    }
  },
  actions: {
    async fetch(params: any) {
      this.user = {
        id: params.id,
        username: params.username,
        roles: params.roles
      }
      this.session = {
        token: params.token,
        type: params.type
      }
      this.loggedIn = true
    },
    clear() {
      this.user = null
      this.session = null
      this.loggedIn = false
    }
  }
})
