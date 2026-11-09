type UserOverview = {
    id: string
    username: string
    fullname: string | null
    roles: string[]
    permissions: string[]
}

export type { UserOverview }
