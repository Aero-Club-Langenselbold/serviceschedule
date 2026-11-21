<template>
  <AppPage
    :title="aircraft ? `${aircraft.registration} · ${aircraft.type}` : $t('maintenance.title')"
    :description="period"
    back="/maintenance"
    :back-label="$t('nav.maintenance.overview')"
    :error-message="errorMessage"
    width="full"
  >
    <template v-if="canManageMaintenance && aircraft" #actions>
      <UButton color="neutral" variant="outline" icon="i-lucide-copy-plus" :label="$t('maintenance.board.importTemplates')" @click="importOpen = true" />
      <UButton color="neutral" variant="outline" icon="i-lucide-pencil" :aria-label="$t('maintenance.aircraft.edit')" @click="aircraftFormOpen = true" />
      <ConfirmationDialog
        variant="outline" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
        :tooltip="$t('maintenance.aircraft.delete')" :modal-title="$t('maintenance.aircraft.deleteConfirm')"
        :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="deleteAircraft"
      />
    </template>

    <UTabs v-model="tab" :items="tabs" :content="false" variant="link" class="mb-5" />

    <!-- Kanban board -->
    <template v-if="tab === 'tasks'">
      <div class="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <!-- every workshop visit has its own board -->
        <div class="flex items-center gap-2">
          <span id="visit-label" class="shrink-0 text-sm text-muted">{{ $t('maintenance.board.visit') }}</span>
          <USelect v-model="visit" :items="visitItems" aria-labelledby="visit-label" class="w-full sm:w-80" data-visit-select />
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <span class="mr-1 text-sm text-muted">{{ $t('maintenance.level.name') }}</span>
          <UButton
            v-for="option in levelFilters" :key="option.value" size="sm" :label="option.label"
            color="neutral" :variant="level === option.value ? 'solid' : 'outline'"
            :aria-pressed="level === option.value" @click="level = option.value"
          />
        </div>
      </div>

      <div class="grid items-start gap-4 lg:grid-cols-3">
        <section
          v-for="status in TASK_STATUSES" :key="status" :aria-label="statusLabel(status)"
          class="rounded-xl bg-elevated/60 p-3 outline-2 outline-offset-2 transition-colors"
          :class="dropTarget === status ? 'bg-primary/10 outline-primary outline-dashed' : 'outline-transparent'"
          @dragover="onDragOver($event, status)" @dragleave="onDragLeave($event, status)" @drop.prevent="onDrop(status)"
        >
          <header class="mb-3 flex h-9 items-center gap-2 px-1">
            <span class="size-2.5 rounded-full" :class="dotClass[status]" />
            <h2 class="font-semibold text-highlighted">{{ statusLabel(status) }}</h2>
            <UBadge color="neutral" variant="subtle" size="sm">{{ column(status).length }}</UBadge>
            <UButton
              v-if="status === 'OPEN' && canManageMaintenance" class="ml-auto" size="sm" color="neutral" variant="outline" icon="i-lucide-plus"
              :aria-label="$t('maintenance.task.add')" @click="openTaskForm(null)"
            />
          </header>
          <ul class="space-y-2.5">
            <li
              v-for="task in column(status)" :key="task.id" :data-task="task.id"
              :draggable="!task.signedAt" :class="{ 'opacity-40': draggedId === task.id }"
              @dragstart="onDragStart($event, task)" @dragend="onDragEnd"
            >
              <!-- a div instead of a button: Firefox does not start a drag on buttons -->
              <div
                role="button" tabindex="0"
                class="flex min-h-26 w-full flex-col gap-2 rounded-lg border border-default bg-default p-3.5 text-left transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary"
                :class="task.signedAt ? 'cursor-pointer' : 'cursor-grab active:cursor-grabbing'"
                :title="task.signedAt ? $t('maintenance.board.locked') : $t('maintenance.board.dragHint')"
                @click="openTask(task.id)" @keydown.enter.prevent="openTask(task.id)" @keydown.space.prevent="openTask(task.id)"
              >
                <span class="flex items-start justify-between gap-2">
                  <span class="font-medium text-highlighted [overflow-wrap:anywhere] hyphens-auto">{{ task.title }}</span>
                  <UIcon v-if="task.status === 'DONE'" name="i-lucide-check" class="size-5 shrink-0 text-success" />
                  <UBadge v-else :color="levelColor(task.level)" variant="outline" size="sm" class="shrink-0">{{ levelLabel(task.level) }}</UBadge>
                </span>
                <span v-if="task.description" class="line-clamp-2 text-sm text-muted">{{ task.description }}</span>
                <span v-if="task.defect" class="flex min-w-0 items-center gap-1.5 rounded-md bg-primary/10 px-2 py-1 text-xs text-primary">
                  <UIcon name="i-lucide-triangle-alert" class="size-3.5 shrink-0" />
                  <span class="truncate">{{ task.defect.title }}</span>
                </span>
                <span class="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted">
                  <span v-if="task.status === 'OPEN' && task.durationMinutes" class="inline-flex items-center gap-1"><UIcon name="i-lucide-clock" class="size-3.5" />{{ formatDuration(task.durationMinutes) }}</span>
                  <span v-if="task.status === 'IN_PROGRESS' && task.checklistTotal" class="inline-flex items-center gap-1"><UIcon name="i-lucide-list-checks" class="size-3.5" />{{ task.checklistDone }}/{{ task.checklistTotal }}</span>
                  <span v-if="task.deadline && task.status !== 'DONE'" class="inline-flex items-center gap-1 font-medium text-warning"><UIcon name="i-lucide-calendar" class="size-3.5" />{{ $t('maintenance.board.deadline', { date: formatDay(task.deadline) }) }}</span>
                  <span v-if="task.noteCount" class="inline-flex items-center gap-1" :title="$t('maintenance.board.notes', task.noteCount)"><UIcon name="i-lucide-message-square" class="size-3.5" />{{ task.noteCount }}</span>
                </span>
                <span v-if="task.status === 'DONE'" class="space-y-0.5 text-xs">
                  <span v-if="task.doneBy" class="block text-muted">{{ $t('maintenance.task.doneBy', { name: task.doneBy.name, date: formatWhen(task.doneAt) }) }}</span>
                  <span v-if="task.signedAt" class="flex items-center gap-1 text-success"><UIcon name="i-lucide-shield-check" class="size-3.5" />{{ $t('maintenance.task.signedBy', { name: task.signedBy?.name ?? '', date: formatWhen(task.signedAt) }) }}</span>
                  <span v-else class="flex items-center gap-1 font-medium text-warning"><UIcon name="i-lucide-clock" class="size-3.5" />{{ $t('maintenance.board.notSigned') }}</span>
                </span>
              </div>
            </li>
            <li v-if="!column(status).length" class="px-1 py-2 text-sm text-muted">{{ $t('maintenance.board.noTasks') }}</li>
          </ul>
        </section>
      </div>
    </template>

    <!-- Defects and findings -->
    <template v-else>
      <div class="mb-4 flex justify-end">
        <UButton icon="i-lucide-triangle-alert" :label="$t('maintenance.defect.add')" @click="openDefectForm(null)" />
      </div>
      <p v-if="!defects.length" class="rounded-xl border border-dashed border-default bg-default p-6 text-center text-muted">{{ $t('maintenance.defect.empty') }}</p>
      <ul v-else class="space-y-3">
        <li v-for="defect in defects" :key="defect.id" class="rounded-xl border border-default bg-default p-4">
          <div class="flex flex-col gap-3 md:flex-row md:items-start">
            <div class="min-w-0 flex-1 space-y-1.5">
              <div class="flex flex-wrap items-center gap-2">
                <span class="font-semibold text-highlighted" :class="{ 'text-muted line-through': defect.status === 'DONE' }">{{ defect.title }}</span>
                <UBadge :color="priorityColor(defect.priority)" variant="subtle" size="sm">{{ priorityLabel(defect.priority) }}</UBadge>
                <UBadge v-if="defect.status === 'DONE'" color="success" variant="subtle" size="sm">{{ $t('maintenance.defect.statusDone') }}</UBadge>
              </div>
              <p v-if="defect.description" class="whitespace-pre-line text-sm text-default">{{ defect.description }}</p>
              <p class="text-xs text-muted">
                {{ $t('maintenance.defect.reportedBy', { name: defect.reportedBy?.name ?? $t('maintenance.task.unknownUser'), date: formatWhen(defect.reportedAt) }) }}
                <template v-if="defect.status === 'DONE' && defect.doneBy"> · {{ $t('maintenance.defect.doneBy', { name: defect.doneBy.name, date: formatWhen(defect.doneAt) }) }}</template>
              </p>
              <UButton v-if="defect.task" color="neutral" variant="link" size="sm" class="-ml-2.5" icon="i-lucide-square-kanban" @click="openTask(defect.task.id)">
                {{ $t('maintenance.defect.task') }}: {{ defect.task.title }} ({{ statusLabel(defect.task.status) }})
              </UButton>
            </div>
            <div class="flex flex-wrap gap-2 md:justify-end">
              <UButton v-if="canManageMaintenance && !defect.task && defect.status === 'OPEN'" color="neutral" variant="outline" size="sm" icon="i-lucide-square-plus" :label="$t('maintenance.defect.createTask')" @click="openTaskForm(null, defect)" />
              <UButton
                color="neutral" variant="outline" size="sm"
                :icon="defect.status === 'OPEN' ? 'i-lucide-check' : 'i-lucide-rotate-ccw'"
                :label="defect.status === 'OPEN' ? $t('maintenance.defect.markDone') : $t('maintenance.defect.markOpen')"
                @click="setDefectStatus(defect, defect.status === 'OPEN' ? 'DONE' : 'OPEN')"
              />
              <template v-if="defect.editable">
                <UButton color="neutral" variant="ghost" size="sm" icon="i-lucide-pencil" :aria-label="$t('maintenance.defect.edit')" @click="openDefectForm(defect)" />
                <ConfirmationDialog
                  variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
                  :tooltip="$t('maintenance.defect.delete')" :modal-title="$t('maintenance.defect.deleteConfirm')"
                  :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="() => deleteDefect(defect)"
                />
              </template>
            </div>
          </div>
        </li>
      </ul>
    </template>

    <MaintenanceTaskDetail v-model:open="detailOpen" :task-id="openTaskId" @changed="refresh" @deleted="refresh" @edit="editTask" />
    <MaintenanceTaskForm
      v-model:open="taskFormOpen" :aircraft-id="aircraftId" :aircraft-label="aircraft ? `${aircraft.registration} · ${aircraft.type}` : undefined"
      :task="editedTask" :defect="defectForTask" :defects="defects" :workshops="visits" :workshop-id="selectedVisit?.id ?? null" @saved="onTaskSaved"
    />
    <MaintenanceTemplateImport
      v-if="aircraft" v-model:open="importOpen" :aircraft="aircraft" :workshop-id="selectedVisit?.id ?? null" :board-label="boardLabel"
      :existing="existingTemplateIds" @imported="onImported"
    />
    <MaintenanceDefectForm v-model:open="defectFormOpen" :aircraft-id="aircraftId" :defect="editedDefect" @saved="refresh" />
    <MaintenanceAircraftForm v-model:open="aircraftFormOpen" :aircraft="aircraft" @saved="refresh" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { Aircraft, Defect, DefectStatus, SkillLevel, TaskDetail, TaskStatus, TaskSummary, WorkshopSlot } from '~/types/maintenance'
import { NO_SELECTION } from '~/composables/useMaintenance'

definePageMeta({
  middleware: ['authenticated']
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const toast = useToast()
const { canManageMaintenance } = storeToRefs(useUserStore())
const { levelLabel, statusLabel, priorityLabel, levelColor, priorityColor, formatDuration, formatDay, formatPeriod, formatWhen } = useMaintenance()

const aircraftId = computed(() => String(route.params.id))
const aircraft = ref<Aircraft | null>(null)
const tasks = ref<TaskSummary[]>([])
const visits = ref<WorkshopSlot[]>([])
const defects = ref<Defect[]>([])
const errorMessage = ref<string | null>(null)

// The tab and an open task live in the URL, so they survive a reload and can be shared.
const tab = computed({
  get: () => (route.query.tab === 'defects' ? 'defects' : 'tasks'),
  set: (value: string) => router.replace({ query: { ...route.query, tab: value === 'defects' ? 'defects' : undefined } })
})
const openDefects = computed(() => defects.value.filter(defect => defect.status === 'OPEN').length)
const tabs = computed(() => [
  { label: t('maintenance.tabs.tasks'), value: 'tasks', icon: 'i-lucide-square-kanban', badge: tasks.value.length || undefined },
  { label: t('maintenance.tabs.defects'), value: 'defects', icon: 'i-lucide-triangle-alert', badge: openDefects.value || undefined }
])

// --- workshop visits: every visit has its own board, tasks outside the workshop are on the board without period

// Without a choice in the URL: the visit running today or else the next one, else the board without period.
// Finished visits only open when chosen explicitly.
const defaultVisit = computed(() => aircraft.value?.workshop?.id ?? NO_SELECTION)
const visit = computed({
  get: () => {
    const chosen = route.query.visit
    return typeof chosen === 'string' && (chosen === NO_SELECTION || visits.value.some(slot => slot.id === chosen)) ? chosen : defaultVisit.value
  },
  set: (value: string) => router.replace({ query: { ...route.query, visit: value } })
})
const selectedVisit = computed(() => visits.value.find(slot => slot.id === visit.value) ?? null)
const visitLabel = (slot: WorkshopSlot) => [formatPeriod(slot.startDate, slot.endDate), slot.note].filter(Boolean).join(' · ')
const boardLabel = computed(() => selectedVisit.value ? visitLabel(selectedVisit.value) : t('maintenance.board.unscheduled'))
const todayIso = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
// running and upcoming visits first (in date order), finished ones below their own heading, latest first
const visitItems = computed(() => {
  const today = todayIso()
  const toItem = (slot: WorkshopSlot) => ({ label: visitLabel(slot), value: slot.id })
  const upcoming = visits.value.filter(slot => slot.endDate >= today).map(toItem)
  const past = visits.value.filter(slot => slot.endDate < today).reverse().map(toItem)
  return [
    ...upcoming,
    { label: t('maintenance.board.unscheduled'), value: NO_SELECTION },
    ...(past.length ? [{ type: 'separator' as const }, { type: 'label' as const, label: t('maintenance.visits.past') }, ...past] : [])
  ]
})

const period = computed(() => {
  if (!aircraft.value) {
    return undefined
  }
  return selectedVisit.value
    ? `${t('maintenance.workshopPeriod')}: ${visitLabel(selectedVisit.value)}`
    : t('maintenance.board.unscheduledDescription')
})

const level = ref<SkillLevel | 'ALL'>('ALL')
const levelFilters = computed(() => [
  { label: t('maintenance.level.all'), value: 'ALL' as const },
  ...SKILL_LEVELS.map(value => ({ label: levelLabel(value), value }))
])
const dotClass: Record<TaskStatus, string> = { OPEN: 'bg-(--ui-text-dimmed)', IN_PROGRESS: 'bg-warning', DONE: 'bg-success' }
const column = (status: TaskStatus) => tasks.value.filter(task => task.status === status && (level.value === 'ALL' || task.level === level.value))

const existingTemplateIds = computed(() => tasks.value.map(task => task.templateId).filter((id): id is string => !!id))

// The latest request wins, so switching visits quickly never shows the board of the previous one.
let taskRequest = 0
const fetchTasks = async () => {
  const request = ++taskRequest
  const workshopId = selectedVisit.value?.id
  const { data, error } = await useAPI<TaskSummary[]>(`/maintenance/aircraft/${aircraftId.value}/tasks`, { method: 'GET', query: workshopId ? { workshopId } : {} })
  if (request !== taskRequest) {
    return
  }
  if (error.value) {
    errorMessage.value = t('maintenance.loadError')
  }
  tasks.value = data.value ?? []
}

const fetchData = async () => {
  const id = aircraftId.value
  const boardBefore = visit.value
  const [aircraftResult, visitResult, defectResult] = await Promise.all([
    useAPI<Aircraft>(`/maintenance/aircraft/${id}`, { method: 'GET' }),
    useAPI<WorkshopSlot[]>('/maintenance/workshop', { method: 'GET', query: { aircraftId: id } }),
    useAPI<Defect[]>(`/maintenance/aircraft/${id}/defects`, { method: 'GET' })
  ])
  if (aircraftResult.error.value?.statusCode === 404) {
    return navigateTo('/maintenance')
  }
  errorMessage.value = aircraftResult.error.value || visitResult.error.value || defectResult.error.value ? t('maintenance.loadError') : null
  aircraft.value = aircraftResult.data.value ?? aircraft.value
  visits.value = (visitResult.data.value ?? []).filter(slot => slot.aircraftId === id)
  defects.value = defectResult.data.value ?? []
  // when the shown board changed (e.g. the default visit is known now), the watcher below loads it
  if (visit.value === boardBefore) {
    await fetchTasks()
  }
}
const refresh = () => fetchData()
watch(visit, fetchTasks)

// --- drag and drop between the columns

const draggedId = ref<string | null>(null)
const dropTarget = ref<TaskStatus | null>(null)
const draggedTask = () => tasks.value.find(task => task.id === draggedId.value)

const onDragStart = (event: DragEvent, task: TaskSummary) => {
  draggedId.value = task.id
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', task.id)
  }
}
const onDragEnd = () => {
  draggedId.value = null
  dropTarget.value = null
}
const onDragOver = (event: DragEvent, status: TaskStatus) => {
  const task = draggedTask()
  if (!task || task.status === status) {
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dropTarget.value = status
}
const onDragLeave = (event: DragEvent, status: TaskStatus) => {
  // dragleave also fires when moving onto a child of the column
  if (dropTarget.value === status && !(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node | null)) {
    dropTarget.value = null
  }
}
const onDrop = async (status: TaskStatus) => {
  const task = draggedTask()
  onDragEnd()
  if (task && task.status !== status) {
    await moveTask(task, status)
  }
}

const moveTask = async (task: TaskSummary, status: TaskStatus) => {
  const previous = task.status
  task.status = status // move the card right away, the answer fills in done/checklist details
  const { data, error } = await useAPI<TaskDetail>(`/maintenance/tasks/${task.id}/status`, { method: 'PUT', body: { status } })
  if (error.value || !data.value) {
    task.status = previous
    toast.add({ title: t('maintenance.board.moveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  await refresh()
}

// --- task dialogs

const openTaskId = computed(() => (typeof route.query.task === 'string' ? route.query.task : null))
const detailOpen = computed({
  get: () => !!openTaskId.value,
  set: (isOpen: boolean) => {
    if (!isOpen) {
      router.replace({ query: { ...route.query, task: undefined } })
    }
  }
})
const openTask = (id: string) => router.replace({ query: { ...route.query, task: id } })

const taskFormOpen = ref(false)
const editedTask = ref<TaskDetail | null>(null)
const defectForTask = ref<Defect | null>(null)
const openTaskForm = (task: TaskDetail | null, defect: Defect | null = null) => {
  editedTask.value = task
  defectForTask.value = defect
  taskFormOpen.value = true
}
const editTask = (task: TaskDetail) => {
  detailOpen.value = false
  openTaskForm(task)
}
// The board switches to the visit of the saved task, so it shows up where it was put.
const onTaskSaved = async (task: TaskDetail) => {
  toast.add({ title: t('maintenance.saved'), icon: 'i-lucide-circle-check', color: 'primary' })
  await router.replace({ query: { ...route.query, visit: task.workshopId ?? NO_SELECTION, task: task.id } })
  await fetchData()
}

const importOpen = ref(false)
const onImported = async (created: number) => {
  toast.add({ title: t('maintenance.board.imported', created), icon: 'i-lucide-circle-check', color: 'primary' })
  await fetchData()
}

// --- defects

const defectFormOpen = ref(false)
const editedDefect = ref<Defect | null>(null)
const openDefectForm = (defect: Defect | null) => {
  editedDefect.value = defect
  defectFormOpen.value = true
}

const setDefectStatus = async (defect: Defect, status: DefectStatus) => {
  const { error } = await useAPI<Defect>(`/maintenance/defects/${defect.id}/status`, { method: 'PUT', body: { status } })
  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
  }
  await fetchData()
}

const deleteDefect = async (defect: Defect) => {
  const { error } = await useAPI(`/maintenance/defects/${defect.id}`, { method: 'DELETE' })
  toast.add(error.value
    ? { title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' }
    : { title: t('maintenance.deleted'), icon: 'i-lucide-circle-check', color: 'primary' })
  await fetchData()
}

// --- aircraft

const aircraftFormOpen = ref(false)
const deleteAircraft = async () => {
  const { error } = await useAPI(`/maintenance/aircraft/${aircraftId.value}`, { method: 'DELETE' })
  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  toast.add({ title: t('maintenance.deleted'), icon: 'i-lucide-circle-check', color: 'primary' })
  await navigateTo('/maintenance')
}

onMounted(fetchData)
</script>
