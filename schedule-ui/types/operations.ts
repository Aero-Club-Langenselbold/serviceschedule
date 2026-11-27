import type { DefectStatus, Person } from '~/types/maintenance'

type ChecklistKind = 'START' | 'END'
type ChecklistStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED'
type ChecklistItemResult = 'OK' | 'NOT_OK'

type ChecklistTemplate = {
    id: string
    aircraftId: string
    kind: ChecklistKind
    items: string[]
    updatedAt: string
}

type ChecklistSummary = {
    id: string
    aircraftId: string
    registration: string
    aircraftType: string
    kind: ChecklistKind
    /** YYYY-MM-DD */
    date: string
    status: ChecklistStatus
    /** the initial recorder */
    createdBy: Person | null
    createdAt: string
    submittedBy: Person | null
    submittedAt: string | null
    approvedBy: Person | null
    approvedAt: string | null
    itemCount: number
    checkedCount: number
    notOkCount: number
}

type ChecklistPoint = {
    id: string
    position: number
    text: string
    result: ChecklistItemResult | null
    comment: string | null
    checkedBy: Person | null
    checkedAt: string | null
    defect: { id: string, title: string, status: DefectStatus } | null
}

type ChecklistDetail = {
    checklist: ChecklistSummary
    items: ChecklistPoint[]
    editable: boolean
    submittable: boolean
    approvable: boolean
    reopenable: boolean
}

type TodayChecklist = {
    aircraftId: string
    registration: string
    aircraftType: string
    kind: ChecklistKind
    date: string
    /** null if the aircraft has no such checklist */
    templateItems: string[] | null
    /** null until somebody starts it */
    checklist: ChecklistDetail | null
}

type AircraftChecklists = {
    aircraftId: string
    registration: string
    aircraftType: string
    startTemplate: ChecklistTemplate | null
    endTemplate: ChecklistTemplate | null
    todayStart: ChecklistSummary | null
    todayEnd: ChecklistSummary | null
}

type OperationsOverview = {
    today: string
    aircraft: AircraftChecklists[]
}

export type {
    AircraftChecklists, ChecklistDetail, ChecklistPoint, ChecklistItemResult, ChecklistKind, ChecklistStatus, ChecklistSummary,
    ChecklistTemplate, OperationsOverview, TodayChecklist
}
