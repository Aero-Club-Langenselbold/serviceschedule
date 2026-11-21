import type { PlanEntry } from './planEntry'
import type { ServiceDate } from './serviceDate'

type Plan = {
    id: string
    name: string
    description: string
    dateFrom: Date | null
    dateUntil: Date | null
    published: boolean
    entries: PlanEntry[]
    serviceDates: ServiceDate[]
}

/** One service of the signed in user (GET /plan/mine), times are local times without zone. */
type PersonalService = {
    id: string
    planId: string
    planName: string
    serviceName: string
    startTime: string
    endTime: string
    colleagues: string[]
}

export type { Plan, PersonalService }

/** A service of a plan with fewer people than required (GET /plan/unfilled/{id}); date as ISO day, times as HH:mm:ss. */
type UnfilledServiceDate = {
    date: string
    serviceId: string
    servicename: string
    startTime: string
    endTime: string
    required: number
    assigned: number
}

export type { UnfilledServiceDate }
