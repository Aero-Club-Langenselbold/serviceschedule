<template>
  <AppPage
    :title="$t('maintenance.defects.title')" :description="$t('maintenance.defects.description')"
    back="/maintenance" :back-label="$t('nav.maintenance.overview')" :error-message="errorMessage" width="full"
  >
    <template #actions>
      <UButton icon="i-lucide-triangle-alert" :label="$t('maintenance.defect.add')" :disabled="!aircraft.length" @click="openForm(null)" />
    </template>

    <div class="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <USelect v-model="aircraftFilter" :items="aircraftItems" :aria-label="$t('maintenance.defect.aircraft')" class="w-full sm:w-72" data-aircraft-filter />
        <USwitch v-model="onlyMine" :label="$t('maintenance.defects.onlyMine')" data-only-mine />
      </div>
      <div class="flex flex-wrap items-center gap-2" role="group" :aria-label="$t('maintenance.status.name')">
        <UButton
          v-for="option in STATUS_FILTERS" :key="option" size="sm" color="neutral"
          :variant="statusFilter === option ? 'solid' : 'outline'" :aria-pressed="statusFilter === option" :data-status-filter="option"
          @click="statusFilter = option"
        >
          {{ $t(`maintenance.defects.filter.${option}`) }}
          <UBadge color="neutral" :variant="statusFilter === option ? 'solid' : 'subtle'" size="sm" class="ml-0.5">{{ counts[option] }}</UBadge>
        </UButton>
      </div>
    </div>

    <p v-if="loaded && !shown.length" class="rounded-xl border border-dashed border-default bg-default p-6 text-center text-muted">
      {{ defects.length ? $t('maintenance.defects.emptyFilter') : $t('maintenance.defect.empty') }}
    </p>
    <ul v-else class="space-y-3">
      <li v-for="defect in shown" :key="defect.id" :data-defect="defect.id">
        <button
          type="button"
          class="flex w-full flex-col gap-2 rounded-xl border border-default bg-default p-4 text-left transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary"
          @click="openDefect(defect.id)"
        >
          <span class="flex flex-wrap items-center gap-2">
            <span class="font-semibold text-highlighted [overflow-wrap:anywhere]" :class="{ 'text-muted line-through': defect.status === 'REJECTED' }">{{ defect.title }}</span>
            <UBadge :color="defectStatusColor(defect)" variant="subtle" size="sm">{{ defectStatusLabel(defect) }}</UBadge>
            <UBadge :color="priorityColor(defect.priority)" variant="outline" size="sm">{{ priorityLabel(defect.priority) }}</UBadge>
            <UBadge v-if="defect.assignedToMe" color="primary" variant="soft" size="sm" icon="i-lucide-user-check">{{ $t('maintenance.defect.assignedToMe') }}</UBadge>
          </span>
          <span v-if="defect.description" class="line-clamp-2 text-sm text-muted">{{ defect.description }}</span>
          <span class="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted">
            <span class="inline-flex items-center gap-1 font-medium text-default"><UIcon name="i-lucide-plane" class="size-3.5" />{{ defect.aircraftLabel }}</span>
            <span class="inline-flex items-center gap-1"><UIcon name="i-lucide-users" class="size-3.5" />{{ defect.assignees.length ? defect.assignees.map(person => person.name).join(', ') : $t('maintenance.defect.unassigned') }}</span>
            <span v-if="defect.commentCount" class="inline-flex items-center gap-1"><UIcon name="i-lucide-message-square" class="size-3.5" />{{ $t('maintenance.defect.comments', defect.commentCount) }}</span>
            <span v-if="defect.task" class="inline-flex items-center gap-1"><UIcon name="i-lucide-square-kanban" class="size-3.5" />{{ defect.task.title }}</span>
            <span>{{ $t('maintenance.defect.reportedBy', { name: defect.reportedBy?.name ?? $t('maintenance.task.unknownUser'), date: formatWhen(defect.reportedAt) }) }}</span>
          </span>
        </button>
      </li>
    </ul>

    <MaintenanceDefectDetail v-model:open="detailOpen" :defect-id="openDefectId" @changed="fetchDefects" @deleted="fetchDefects" @edit="editDefect" />
    <MaintenanceDefectForm
      v-model:open="formOpen" :aircraft="aircraft" :aircraft-id="formAircraftId" :defect="editedDefect" @saved="onSaved"
    />
  </AppPage>
</template>

<script lang="ts" setup>
import type { Aircraft, Defect, DefectDetail, DefectPriority } from '~/types/maintenance'

definePageMeta({
  middleware: ['authenticated']
})

type StatusFilter = 'OPEN' | 'CLOSED' | 'SIGNED' | 'ALL'
const STATUS_FILTERS: StatusFilter[] = ['OPEN', 'CLOSED', 'SIGNED', 'ALL']
const ALL_AIRCRAFT = 'all'
const PRIORITY_RANK: Record<DefectPriority, number> = { HIGH: 0, NORMAL: 1, LOW: 2, VERY_LOW: 3 }

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const toast = useToast()
const { defectStatusLabel, defectStatusColor, isDefectOpen, priorityLabel, priorityColor, formatWhen } = useMaintenance()

const defects = ref<Defect[]>([])
const aircraft = ref<Aircraft[]>([])
const loaded = ref(false)
const errorMessage = ref<string | null>(null)

// Filters and the open defect live in the URL, so a notification or a shared link opens exactly this view.
const queryValue = (key: string) => (typeof route.query[key] === 'string' ? route.query[key] as string : undefined)
const setQuery = (values: Record<string, string | undefined>) => router.replace({ query: { ...route.query, ...values } })

const aircraftFilter = computed({
  get: () => {
    const chosen = queryValue('aircraft')
    return chosen && aircraft.value.some(item => item.id === chosen) ? chosen : ALL_AIRCRAFT
  },
  set: (value: string) => setQuery({ aircraft: value === ALL_AIRCRAFT ? undefined : value })
})
const statusFilter = computed({
  get: () => (STATUS_FILTERS.find(option => option === queryValue('status')) ?? 'OPEN'),
  set: (value: StatusFilter) => setQuery({ status: value === 'OPEN' ? undefined : value })
})
const onlyMine = computed({
  get: () => queryValue('mine') === '1',
  set: (value: boolean) => setQuery({ mine: value ? '1' : undefined })
})

const aircraftItems = computed(() => [
  { label: t('maintenance.defects.allAircraft'), value: ALL_AIRCRAFT },
  ...aircraft.value.map(item => ({ label: `${item.registration} · ${item.type}`, value: item.id }))
])

const matchesStatus = (defect: Defect, filter: StatusFilter) => {
  switch (filter) {
    case 'OPEN': return isDefectOpen(defect)
    case 'CLOSED': return !isDefectOpen(defect) && !defect.signedAt
    case 'SIGNED': return !!defect.signedAt
    default: return true
  }
}
// aircraft and "mine" narrow the list first, the status buttons count within that
const narrowed = computed(() => defects.value.filter(defect =>
  (aircraftFilter.value === ALL_AIRCRAFT || defect.aircraftId === aircraftFilter.value) && (!onlyMine.value || defect.assignedToMe)))
const counts = computed(() => Object.fromEntries(STATUS_FILTERS.map(option =>
  [option, narrowed.value.filter(defect => matchesStatus(defect, option)).length])) as Record<StatusFilter, number>)
// open defects by urgency, everything else latest first (as the server sends them)
const shown = computed(() => {
  const list = narrowed.value.filter(defect => matchesStatus(defect, statusFilter.value))
  return statusFilter.value === 'OPEN' ? [...list].sort((a, b) => PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority]) : list
})

const fetchDefects = async () => {
  const { data, error } = await useAPI<Defect[]>('/maintenance/defects', { method: 'GET' })
  errorMessage.value = error.value ? t('maintenance.loadError') : null
  defects.value = data.value ?? []
}

const fetchData = async () => {
  const [aircraftResult] = await Promise.all([
    useAPI<Aircraft[]>('/maintenance/aircraft', { method: 'GET' }),
    fetchDefects()
  ])
  if (aircraftResult.error.value) {
    errorMessage.value = t('maintenance.loadError')
  }
  aircraft.value = aircraftResult.data.value ?? []
  loaded.value = true
}

// --- detail and form

const openDefectId = computed(() => queryValue('defect') ?? null)
const detailOpen = computed({
  get: () => !!openDefectId.value,
  set: (isOpen: boolean) => {
    if (!isOpen) {
      setQuery({ defect: undefined })
    }
  }
})
const openDefect = (id: string) => setQuery({ defect: id })

const formOpen = ref(false)
const editedDefect = ref<Defect | null>(null)
// a new defect is reported for the aircraft the list is filtered to
const formAircraftId = computed(() => editedDefect.value ? editedDefect.value.aircraftId : (aircraftFilter.value === ALL_AIRCRAFT ? null : aircraftFilter.value))
const openForm = (defect: Defect | null) => {
  editedDefect.value = defect
  formOpen.value = true
}
const editDefect = (defect: Defect) => {
  detailOpen.value = false
  openForm(defect)
}
const onSaved = async (detail: DefectDetail) => {
  toast.add({ title: t('maintenance.saved'), icon: 'i-lucide-circle-check', color: 'primary' })
  await setQuery({ defect: detail.defect.id })
  await fetchDefects()
}

onMounted(fetchData)
</script>
