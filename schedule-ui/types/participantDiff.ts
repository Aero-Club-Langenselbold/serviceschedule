import type { Participant } from './participant'

type ParticipantDiff = {
    newParticipants: Participant[]
    updatedParticipants: Participant[]
    unchangedParticipants: Participant[]
    missingParticipants: Participant[]
}

export type { ParticipantDiff }
