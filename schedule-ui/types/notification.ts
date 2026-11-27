import type { DefectStatus, Person } from '~/types/maintenance'
import type { ChecklistKind, ChecklistStatus } from '~/types/operations'

type NotificationType = 'DEFECT_ASSIGNED' | 'CHECKLIST_SUBMITTED'

type UserNotification = {
    id: string
    type: NotificationType
    createdAt: string
    read: boolean
    actor: Person | null
    defect: { id: string, title: string, aircraftId: string, aircraftLabel: string, status: DefectStatus } | null
    checklist?: { id: string, aircraftId: string, registration: string, kind: ChecklistKind, date: string, status: ChecklistStatus } | null
}

type Notifications = {
    unreadCount: number
    items: UserNotification[]
}

export type { NotificationType, UserNotification, Notifications }
