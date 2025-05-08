import type { Participant } from './participant'
import type { Plan } from './plan'
import type { Service } from './service'

type ParticipantAvailability = {
  id: string
  date: Date
  requestedService: Service | null
  participant: Participant
  Plan: Plan
}

export type { ParticipantAvailability }
