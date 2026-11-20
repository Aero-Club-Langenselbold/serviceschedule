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
    workshopStart: string | null
    workshopEnd: string | null
    taskCount: number
    doneCount: number
    signedCount: number
    openDefectCount: number
}

type AircraftInput = {
    type: string
    registration: string
    workshopStart: string | null
    workshopEnd: string | null
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

type TaskDetail = Omit<TaskSummary, 'checklistTotal' | 'checklistDone' | 'noteCount'> & {
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
    Aircraft, AircraftInput, TaskTemplate, TaskTemplateInput, TaskSummary, ChecklistItem, TaskNote, TaskHistoryEntry,
    TaskDetail, TaskInput, Defect, DefectInput
}
