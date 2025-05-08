import type { Participant } from './participant'
import type { Service } from './service'

type PlanEntry = {
    id: string
    date: Date
    startTime: Date
    endTime: Date
    service: Service
    participant: Participant
}

export type { PlanEntry }

