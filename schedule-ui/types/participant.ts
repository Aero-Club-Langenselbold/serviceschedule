import type { ParticipantService } from './participantService'

type Participant = {
    id: string
    firstname: string
    lastname: string
    fullname: string
    email: string
    dateOfBirth: Date | undefined
    status: 'ACTIVE' | 'INACTIVE'
    minAssignments: number
    maxAssignments: number
    registrationKey: string
    participantServices: ParticipantService[] | undefined
    services: undefined
}

export type { Participant }

