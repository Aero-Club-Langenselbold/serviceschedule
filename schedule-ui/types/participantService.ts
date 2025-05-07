import type { Participant } from './participant'
import type { Service } from './service'

type ParticipantService = {
    participant: Participant | null
    service: Service
    autoAssign: boolean
}

export type { ParticipantService }

