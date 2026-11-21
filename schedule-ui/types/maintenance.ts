type SkillLevel = 'BEGINNER' | 'EXPERIENCED' | 'WORKSHOP_LEAD'
type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE'
type DefectPriority = 'VERY_LOW' | 'LOW' | 'NORMAL' | 'HIGH'
type DefectStatus = 'OPEN' | 'DONE'
type TaskHistoryAction = 'CREATED' | 'UPDATED' | 'STARTED' | 'RESET' | 'FINISHED' | 'REOPENED' | 'SIGNED' | 'UNSIGNED' | 'NOTE_ADDED' | 'NOTE_DELETED'

/** A user as the API names them: participant name if known, otherwise the username. */
type Person = {
    id: string
    name: string
}

type Aircraft = {
    id: string
    type: string
    registration: string
    /** The workshop period running today or else the next one, with its progress. */
    workshop: WorkshopSlot | null
    /** Tasks on the board without workshop period. */
    unscheduledTaskCount: number
    openDefectCount: number
}

type AircraftInput = {
    type: string
    registration: string
}

/** A workshop visit: a period in which an aircraft is planned into the workshop; dates as YYYY-MM-DD, both inclusive. Every visit has its own board. */
type WorkshopSlot = {
    id: string
    aircraftId: string
    startDate: string
    endDate: string
    note: string | null
    taskCount: number
    doneCount: number
    signedCount: number
    /** Checklist steps of the tasks of the visit; a task without checklist counts as one step. */
    stepCount: number
    stepDoneCount: number
}

type WorkshopSlotInput = Pick<WorkshopSlot, 'aircraftId' | 'startDate' | 'endDate' | 'note'>

type InventoryKind = 'CONSUMABLE' | 'SPARE_PART'
/** COUNT: number of units; LEVEL: 0 = empty, 1 = low, 2 = enough. */
type InventoryCounting = 'COUNT' | 'LEVEL'
type InventoryStockStatus = 'OK' | 'LOW' | 'EMPTY'
type InventoryHistoryAction = 'CREATED' | 'UPDATED' | 'COUNTED'

type InventoryItem = {
    id: string
    name: string
    kind: InventoryKind
    location: string | null
    counting: InventoryCounting
    quantity: number
    minQuantity: number | null
    unit: string | null
    comment: string | null
    /** Only spare parts; empty means every aircraft. */
    aircraftTypes: string[]
    status: InventoryStockStatus
}

type InventoryItemInput = Omit<InventoryItem, 'id' | 'status'>

type InventoryHistoryEntry = {
    id: string
    action: InventoryHistoryAction
    quantity: number | null
    actor: Person | null
    createdAt: string
}

type TaskTemplate = {
    id: string
    title: string
    description: string | null
    level: SkillLevel
    durationMinutes: number | null
    aircraftTypes: string[]
    checklist: string[]
}

type TaskTemplateInput = Omit<TaskTemplate, 'id'>

type TaskSummary = {
    id: string
    aircraftId: string
    /** Workshop visit whose board shows the task, null for the board without period. */
    workshopId: string | null
    templateId: string | null
    title: string
    description: string | null
    level: SkillLevel
    status: TaskStatus
    durationMinutes: number | null
    deadline: string | null
    fromTemplate: boolean
    defect: { id: string, title: string, status: DefectStatus } | null
    checklistTotal: number
    checklistDone: number
    noteCount: number
    doneBy: Person | null
    doneAt: string | null
    signedBy: Person | null
    signedAt: string | null
}

type ChecklistItem = {
    id: string
    text: string
    done: boolean
    doneBy: Person | null
    doneAt: string | null
}

type TaskNote = {
    id: string
    content: string
    author: Person | null
    createdAt: string
    deletable: boolean
}

type TaskHistoryEntry = {
    id: string
    action: TaskHistoryAction
    actor: Person | null
    createdAt: string
}

type TaskDetail = Omit<TaskSummary, 'templateId' | 'checklistTotal' | 'checklistDone' | 'noteCount'> & {
    aircraftLabel: string
    checklist: ChecklistItem[]
    notes: TaskNote[]
    history: TaskHistoryEntry[]
}

type TaskInput = {
    title: string
    description: string | null
    level: SkillLevel
    durationMinutes: number | null
    deadline: string | null
    defectId: string | null
    workshopId: string | null
    checklist: string[]
}

type Defect = {
    id: string
    aircraftId: string
    title: string
    description: string | null
    priority: DefectPriority
    status: DefectStatus
    reportedBy: Person | null
    reportedAt: string
    doneBy: Person | null
    doneAt: string | null
    task: { id: string, title: string, status: TaskStatus } | null
    editable: boolean
}

type DefectInput = {
    title: string
    description: string | null
    priority: DefectPriority
}

export type {
    SkillLevel, TaskStatus, DefectPriority, DefectStatus, TaskHistoryAction, Person,
    Aircraft, AircraftInput, WorkshopSlot, WorkshopSlotInput,
    InventoryKind, InventoryCounting, InventoryStockStatus, InventoryHistoryAction, InventoryItem, InventoryItemInput, InventoryHistoryEntry,
    TaskTemplate, TaskTemplateInput, TaskSummary, ChecklistItem, TaskNote, TaskHistoryEntry,
    TaskDetail, TaskInput, Defect, DefectInput
}
