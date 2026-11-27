import type { Defect, DefectPriority, DefectStatus, InventoryItem, InventoryStockStatus, SkillLevel, TaskStatus } from '~/types/maintenance'

type BadgeColor = 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'

export const SKILL_LEVELS: SkillLevel[] = ['BEGINNER', 'EXPERIENCED', 'WORKSHOP_LEAD']
export const TASK_STATUSES: TaskStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE']
export const DEFECT_PRIORITIES: DefectPriority[] = ['HIGH', 'NORMAL', 'LOW', 'VERY_LOW']
export const DURATIONS = [15, 30, 60, 90, 120, 180, 240, 480]

export const NO_SELECTION = 'none'

/** Fill levels of items counted as LEVEL, from full to empty. */
export const INVENTORY_LEVELS = [2, 1, 0] as const

/** Select items need a value: 0 stands for "no duration". */
export const toDuration = (minutes: number | null | undefined) => minutes ?? 0
export const fromDuration = (minutes: number) => minutes || null

/** One checklist step per line, blank lines dropped. */
export const splitLines = (text: string) => text.split('\n').map(line => line.trim()).filter(Boolean)

const LEVEL_COLORS: Record<SkillLevel, BadgeColor> = { BEGINNER: 'success', EXPERIENCED: 'info', WORKSHOP_LEAD: 'secondary' }
const STATUS_COLORS: Record<TaskStatus, BadgeColor> = { OPEN: 'neutral', IN_PROGRESS: 'warning', DONE: 'success' }
const PRIORITY_COLORS: Record<DefectPriority, BadgeColor> = { HIGH: 'error', NORMAL: 'warning', LOW: 'info', VERY_LOW: 'neutral' }
const DEFECT_STATUS_COLORS: Record<DefectStatus | 'SIGNED', BadgeColor> = { OPEN: 'error', IN_PROGRESS: 'warning', DONE: 'success', REJECTED: 'neutral', SIGNED: 'primary' }
const STOCK_COLORS: Record<InventoryStockStatus, BadgeColor> = { OK: 'success', LOW: 'warning', EMPTY: 'error' }

/** Labels, colors and formatting shared by the maintenance pages. */
export function useMaintenance() {
    const { t, locale } = useI18n()

    const levelLabel = (level: SkillLevel) => t(`maintenance.level.${level}`)
    const statusLabel = (status: TaskStatus) => t(`maintenance.status.${status}`)
    const priorityLabel = (priority: DefectPriority) => t(`maintenance.priority.${priority}`)

    /** A signed off defect shows as such, whatever it was closed with. */
    const defectState = (defect: Pick<Defect, 'status' | 'signedAt'>): DefectStatus | 'SIGNED' => defect.signedAt ? 'SIGNED' : defect.status
    const defectStatusLabel = (defect: Pick<Defect, 'status' | 'signedAt'>) => t(`maintenance.defect.status.${defectState(defect)}`)
    const defectStatusColor = (defect: Pick<Defect, 'status' | 'signedAt'>) => DEFECT_STATUS_COLORS[defectState(defect)]
    const isDefectOpen = (defect: Pick<Defect, 'status'>) => defect.status === 'OPEN' || defect.status === 'IN_PROGRESS'

    const levelColor = (level: SkillLevel) => LEVEL_COLORS[level]
    const statusColor = (status: TaskStatus) => STATUS_COLORS[status]
    const priorityColor = (priority: DefectPriority) => PRIORITY_COLORS[priority]

    const formatDuration = (minutes: number | null | undefined) => {
        if (!minutes) {
            return ''
        }
        if (minutes < 60) {
            return t('maintenance.duration.minutes', { count: minutes })
        }
        const hours = minutes / 60
        return t('maintenance.duration.hours', { count: Number.isInteger(hours) ? hours : hours.toLocaleString(locale.value, { maximumFractionDigits: 1 }) })
    }

    // Dates without time come as YYYY-MM-DD and are shown as they are, without any time zone shift.
    const formatDay = (day: string | null | undefined) => {
        if (!day) {
            return ''
        }
        const [year, month, date] = day.split('-').map(Number)
        return new Date(year!, month! - 1, date!).toLocaleDateString(locale.value, { day: '2-digit', month: '2-digit', year: 'numeric' })
    }

    const formatWhen = (timestamp: string | null | undefined) => timestamp
        ? new Date(timestamp).toLocaleString(locale.value, { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
        : ''

    const stockColor = (status: InventoryStockStatus) => STOCK_COLORS[status]
    const levelName = (level: number) => t(`maintenance.inventory.levels.${level}`)
    const formatStock = (item: Pick<InventoryItem, 'counting' | 'quantity' | 'unit'>) => item.counting === 'LEVEL'
        ? levelName(item.quantity)
        : [item.quantity.toLocaleString(locale.value), item.unit].filter(Boolean).join(' ')

    const formatPeriod = (start: string | null | undefined, end: string | null | undefined) => `${formatDay(start)} – ${formatDay(end)}`

    /** Share of ticked checklist steps in percent, rounded down so 100 % only shows when everything is done. */
    const progressPercent = (aircraft: { stepCount: number, stepDoneCount: number }) =>
        aircraft.stepCount ? Math.floor(aircraft.stepDoneCount * 100 / aircraft.stepCount) : 0

    // Select items need a value, 0 stands for "not specified" (see toDuration/fromDuration).
    const durationItems = computed(() => [
        { label: t('maintenance.duration.none'), value: 0 },
        ...DURATIONS.map(minutes => ({ label: formatDuration(minutes), value: minutes }))
    ])
    const levelItems = computed(() => SKILL_LEVELS.map(level => ({ label: levelLabel(level), value: level })))
    const priorityItems = computed(() => DEFECT_PRIORITIES.map(priority => ({ label: priorityLabel(priority), value: priority })))

    return {
        defectStatusLabel, defectStatusColor, isDefectOpen,
        levelLabel, statusLabel, priorityLabel, levelColor, statusColor, priorityColor,
        stockColor, levelName, formatStock, formatDuration, formatDay, formatPeriod, formatWhen, progressPercent, durationItems, levelItems, priorityItems
    }
}
