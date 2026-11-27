import type { ChecklistKind, ChecklistStatus } from '~/types/operations'

type BadgeColor = 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'

export const CHECKLIST_KINDS: ChecklistKind[] = ['START', 'END']
export const CHECKLIST_STATUSES: ChecklistStatus[] = ['IN_PROGRESS', 'SUBMITTED', 'APPROVED']

const STATUS_COLORS: Record<ChecklistStatus, BadgeColor> = { IN_PROGRESS: 'warning', SUBMITTED: 'info', APPROVED: 'success' }

/** The fixed link behind the QR code of an aircraft and kind; it always opens the checklist of the current day. */
export const todayPath = (aircraftId: string, kind: ChecklistKind) => `/operations/checklist/${aircraftId}/${kind.toLowerCase()}`

/** Route parameter (start/end) to kind, null for anything else. */
export const parseKind = (value: unknown): ChecklistKind | null => {
    const kind = String(value ?? '').toUpperCase()
    return kind === 'START' || kind === 'END' ? kind : null
}

/** Labels and colors shared by the operations checklist pages. */
export function useOperations() {
    const { t } = useI18n()

    const kindLabel = (kind: ChecklistKind) => t(`operations.kind.${kind}`)
    const statusLabel = (status: ChecklistStatus) => t(`operations.status.${status}`)
    const statusColor = (status: ChecklistStatus) => STATUS_COLORS[status]

    const kindItems = computed(() => CHECKLIST_KINDS.map(kind => ({ label: kindLabel(kind), value: kind })))
    const statusItems = computed(() => CHECKLIST_STATUSES.map(status => ({ label: statusLabel(status), value: status })))

    /** The absolute URL for the QR code, so a phone camera opens the page directly. */
    const todayUrl = (aircraftId: string, kind: ChecklistKind) => {
        const base = import.meta.client ? window.location.origin : ''
        return `${base}${useRuntimeConfig().app.baseURL.replace(/\/$/, '')}${todayPath(aircraftId, kind)}`
    }

    return { kindLabel, statusLabel, statusColor, kindItems, statusItems, todayUrl }
}
