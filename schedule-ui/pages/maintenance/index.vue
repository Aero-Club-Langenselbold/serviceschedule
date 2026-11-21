<template>
  <AppPage :title="$t('maintenance.title')" :description="$t('maintenance.description')" :error-message="errorMessage">
    <template v-if="canManageMaintenance" #actions>
      <UButton color="neutral" variant="outline" icon="i-lucide-chart-gantt" to="/maintenance/schedule" :label="$t('nav.maintenance.schedule')" />
      <UButton color="neutral" variant="outline" icon="i-lucide-copy" to="/maintenance/templates" :label="$t('nav.maintenance.templates')" />
      <UButton icon="i-lucide-plus" :label="$t('maintenance.aircraft.add')" @click="formOpen = true" />
    </template>

    <div v-if="loaded && !aircraft.length" class="rounded-xl border border-dashed border-default bg-default p-8 text-center">
      <UIcon name="i-lucide-plane" class="mx-auto mb-2 size-8 text-dimmed" />
      <p class="font-medium text-highlighted">{{ $t('maintenance.empty') }}</p>
      <p v-if="canManageMaintenance" class="mt-1 text-sm text-muted">{{ $t('maintenance.emptyAdmin') }}</p>
    </div>

    <ul v-else class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <li v-for="item in aircraft" :key="item.id" :data-aircraft="item.id" class="flex h-full flex-col gap-4 rounded-xl border border-default bg-default p-4">
        <NuxtLink :to="`/maintenance/${item.id}`" class="-m-1 flex items-start gap-3 rounded-lg p-1 hover:bg-elevated/60 focus-visible:outline-2 focus-visible:outline-primary">
          <span class="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <UIcon name="i-lucide-plane" class="size-5" />
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate font-semibold text-highlighted">{{ item.registration }}</span>
            <span class="block truncate text-sm text-muted">{{ item.type }}</span>
          </span>
          <UIcon name="i-lucide-chevron-right" class="mt-1 size-4 shrink-0 text-dimmed" />
        </NuxtLink>

        <!-- every workshop visit has its own board: running and upcoming visits directly, finished ones on request -->
        <div class="space-y-2">
          <p v-if="!upcoming(item).length" class="flex items-center gap-1.5 text-sm text-muted">
            <UIcon name="i-lucide-calendar-range" class="size-4 shrink-0" />{{ $t('maintenance.noPeriod') }}
          </p>
          <NuxtLink
            v-for="slot in visibleVisits(item)" :key="slot.id" :to="`/maintenance/${item.id}?visit=${slot.id}`" :data-visit="slot.id"
            class="block space-y-1.5 rounded-lg border border-default p-2.5 transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary"
            :class="{ 'bg-elevated/50': isPast(slot) }"
          >
            <span class="flex items-center gap-1.5 text-sm" :class="isPast(slot) ? 'text-muted' : 'text-highlighted'">
              <UIcon :name="isPast(slot) ? 'i-lucide-calendar-check' : 'i-lucide-calendar-range'" class="size-4 shrink-0" />
              <span class="truncate">{{ formatPeriod(slot.startDate, slot.endDate) }}<template v-if="slot.note"> · {{ slot.note }}</template></span>
              <UBadge v-if="isRunning(slot)" color="primary" variant="subtle" size="sm" class="ml-auto shrink-0">{{ $t('maintenance.visits.running') }}</UBadge>
            </span>
            <template v-if="slot.taskCount">
              <span class="flex items-baseline justify-between gap-2 text-xs">
                <span class="text-default">{{ $t('maintenance.progress', { done: slot.doneCount, total: slot.taskCount }) }}</span>
                <span v-if="slot.signedCount" class="text-success">{{ $t('maintenance.signedProgress', { count: slot.signedCount }) }}</span>
              </span>
              <UProgress
                :model-value="slot.stepDoneCount" :max="Math.max(slot.stepCount, 1)" size="xs"
                :color="slot.stepCount && slot.stepDoneCount === slot.stepCount ? 'success' : 'primary'"
                :aria-label="$t('maintenance.schedule.progress', { percent: progressPercent(slot) })"
              />
            </template>
            <span v-else class="block text-xs text-muted">{{ $t('maintenance.schedule.noTasks') }}</span>
          </NuxtLink>
          <UButton
            v-if="past(item).length" color="neutral" variant="link" size="sm" class="px-0" data-show-past
            :icon="showPast.has(item.id) ? 'i-lucide-chevron-up' : 'i-lucide-history'"
            :label="showPast.has(item.id) ? $t('maintenance.visits.hidePast') : $t('maintenance.visits.showPast', past(item).length)"
            @click="togglePast(item.id)"
          />
        </div>

        <div class="mt-auto flex flex-wrap items-center gap-2">
          <UButton
            v-if="item.unscheduledTaskCount" :to="`/maintenance/${item.id}?visit=${NO_SELECTION}`" color="neutral" variant="outline" size="sm"
            icon="i-lucide-square-kanban" :label="$t('maintenance.unscheduledTasks', item.unscheduledTaskCount)"
          />
          <UBadge v-if="item.openDefectCount" color="warning" variant="subtle" icon="i-lucide-triangle-alert">
            {{ $t('maintenance.openDefects', item.openDefectCount) }}
          </UBadge>
        </div>
      </li>
    </ul>

    <MaintenanceAircraftForm v-model:open="formOpen" @saved="onSaved" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { Aircraft, WorkshopSlot } from '~/types/maintenance'
import { NO_SELECTION } from '~/composables/useMaintenance'

definePageMeta({
  middleware: ['authenticated']
})

const { t } = useI18n()
const { canManageMaintenance } = storeToRefs(useUserStore())
const { formatPeriod, progressPercent } = useMaintenance()

const aircraft = ref<Aircraft[]>([])
const visits = ref<WorkshopSlot[]>([])
const loaded = ref(false)
const errorMessage = ref<string | null>(null)
const formOpen = ref(false)

// ISO dates (YYYY-MM-DD) compare correctly as strings; today in local time
const todayIso = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
const today = ref(todayIso())
const isPast = (slot: WorkshopSlot) => slot.endDate < today.value
const isRunning = (slot: WorkshopSlot) => slot.startDate <= today.value && !isPast(slot)

const visitsOf = (item: Aircraft) => visits.value.filter(slot => slot.aircraftId === item.id)
const upcoming = (item: Aircraft) => visitsOf(item).filter(slot => !isPast(slot))
// latest finished visit first
const past = (item: Aircraft) => visitsOf(item).filter(isPast).reverse()

const showPast = ref(new Set<string>())
const togglePast = (id: string) => {
  const next = new Set(showPast.value)
  if (!next.delete(id)) {
    next.add(id)
  }
  showPast.value = next
}
const visibleVisits = (item: Aircraft) => showPast.value.has(item.id) ? [...upcoming(item), ...past(item)] : upcoming(item)

const fetchData = async () => {
  const [aircraftResult, visitResult] = await Promise.all([
    useAPI<Aircraft[]>('/maintenance/aircraft', { method: 'GET' }),
    useAPI<WorkshopSlot[]>('/maintenance/workshop', { method: 'GET' })
  ])
  const failed = !!aircraftResult.error.value || !!visitResult.error.value
  errorMessage.value = failed ? t('maintenance.loadError') : null
  aircraft.value = aircraftResult.data.value ?? []
  visits.value = visitResult.data.value ?? []
  today.value = todayIso()
  loaded.value = !failed
}

const onSaved = (created: Aircraft) => navigateTo(`/maintenance/${created.id}`)

onMounted(fetchData)
</script>
