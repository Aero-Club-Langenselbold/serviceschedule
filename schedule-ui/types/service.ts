import type { Settings } from './settings'

type Service = {
    id: string
    servicename: string
    addAtImport: boolean
    settings: Settings[]
}

export type { Service }

