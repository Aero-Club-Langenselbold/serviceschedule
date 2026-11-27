import type { DefectStatus, Person } from '~/types/maintenance'

type NotificationType = 'DEFECT_ASSIGNED'

type UserNotification = {
    id: string
    type: NotificationType
    createdAt: string
    read: boolean
    actor: Person | null
    defect: { id: string, title: string, aircraftId: string, aircraftLabel: string, status: DefectStatus } | null
}

type Notifications = {
    unreadCount: number
    items: UserNotification[]
}

export type { NotificationType, UserNotification, Notifications }
