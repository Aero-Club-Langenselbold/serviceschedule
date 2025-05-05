import type { State } from './state'

type Country = {
    id: string
    name: string
    states: State[]
}

export type { Country }
