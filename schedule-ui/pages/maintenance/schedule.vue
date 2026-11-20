<template>
  <AppPage
    :title="$t('maintenance.schedule.title')" :description="$t('maintenance.schedule.description')"
    back="/maintenance" :back-label="$t('nav.maintenance.overview')" :error-message="errorMessage" width="full"
  >
    <template v-if="canManageMaintenance" #actions>
      <UButton icon="i-lucide-calendar-plus" :label="$t('maintenance.schedule.add')" :disabled="!aircraft.length" @click="openForm(null)" />
    </template>

    <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
      <USwitch v-model="showPast" :label="$t('maintenance.schedule.showPast')" />
      <ul class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted">
        <li class="flex items-center gap-1.5"><span class="h-3 w-5 rounded-sm border border-primary/50 bg-primary/15" />{{ $t('maintenance.schedule.legend.planned') }}</li>
        <li class="flex items-center gap-1.5"><span class="h-3 w-5 rounded-sm border border-primary/50 bg-linear-to-r from-primary/45 from-50% to-primary/15 to-50%" />{{ $t('maintenance.schedule.legend.progress') }}</li>
        <li class="flex items-center gap-1.5"><span class="h-3 w-5 rounded-sm border border-default bg-elevated" />{{ $t('maintenance.schedule.legend.past') }}</li>
        <li class="flex items-center gap-1.5"><span class="h-3 w-0.5 bg-error" />{{ $t('maintenance.schedule.legend.today') }}</li>
      </ul>
    </div>

    <div v-if="loaded && !aircraft.length" class="rounded-xl border border-dashed border-default bg-default p-8 text-center">
      <UIcon name="i-lucide-plane" class="mx-auto mb-2 size-8 text-dimmed" />
      <p class="font-medium text-highlighted">{{ $t('maintenance.empty') }}</p>
    </div>

    <template v-else-if="loaded">
      <!-- Gantt chart: one row per aircraft, one bar per workshop period -->
      <div class="overflow-x-auto rounded-xl border border-default bg-default" role="figure" :aria-label="$t('maintenance.schedule.chart')">
        <div class="min-w-max">
          <div class="flex border-b border-default">
            <div class="sticky left-0 z-20 flex w-36 shrink-0 items-end border-r border-default bg-default px-3 py-1.5 text-xs font-medium text-muted sm:w-56">
              {{ $t('maintenance.schedule.aircraft') }}
            </div>
            <div :style="{ width: px(totalDays) }">
              <div class="flex h-7 border-b border-default">
                <div
                  v-for="month in months" :key="month.key"
                  class="shrink-0 truncate border-l border-default px-2 text-xs leading-7 font-medium text-highlighted first:border-l-0"
                  :style="{ width: px(month.days) }"
                >{{ month.label }}</div>
              </div>
              <div class="flex h-6">
                <div
                  v-for="week in weeks" :key="week.offset"
                  class="shrink-0 truncate border-l border-default px-1.5 text-[11px] leading-6 text-muted first:border-l-0"
                  :style="{ width: px(7) }"
                >{{ $t('maintenance.schedule.week', { week: week.number }) }}</div>
              </div>
            </div>
          </div>

          <div v-for="row in rows" :key="row.aircraft.id" :data-row="row.aircraft.id" class="flex border-b border-default last:border-b-0">
            <NuxtLink
              :to="`/maintenance/${row.aircraft.id}`"
              class="sticky left-0 z-10 flex w-36 shrink-0 flex-col justify-center gap-1 border-r border-default bg-default px-3 py-2 hover:bg-elevated focus-visible:outline-2 focus-visible:outline-primary sm:w-56"
            >
              <span class="flex items-baseline justify-between gap-2">
                <span class="truncate text-sm font-semibold text-highlighted">{{ row.aircraft.registration }}</span>
                <span class="shrink-0 text-xs font-medium" :class="row.percent === 100 ? 'text-success' : 'text-default'">{{ row.percent }} %</span>
              </span>
              <span class="hidden truncate text-xs text-muted sm:block">
                {{ row.aircraft.type }} · {{ $t('maintenance.schedule.steps', { done: row.aircraft.stepDoneCount, total: row.aircraft.stepCount }) }}
              </span>
              <UProgress
                :model-value="row.aircraft.stepDoneCount" :max="Math.max(row.aircraft.stepCount, 1)" size="xs"
                :color="row.percent === 100 ? 'success' : 'primary'" :aria-label="$t('maintenance.schedule.progress', { percent: row.percent })"
              />
            </NuxtLink>

            <div
              class="relative h-16 shrink-0" :class="{ 'cursor-copy': canManageMaintenance }" :style="{ width: px(totalDays) }"
              :title="canManageMaintenance ? $t('maintenance.schedule.clickToPlan') : undefined"
              @click="onTrackClick($event, row.aircraft)"
            >
              <div v-for="week in weeks" :key="week.offset" class="pointer-events-none absolute inset-y-0 border-l border-default/70 first:border-l-0" :style="{ left: px(week.offset) }" />
              <div v-if="todayOffset >= 0" class="pointer-events-none absolute inset-y-0 z-[1] w-0.5 bg-error/80" :style="{ left: px(todayOffset + 0.5) }" />
              <button
                v-for="slot in row.slots" :key="slot.id" type="button" :data-slot="slot.id"
                class="absolute top-3 bottom-3 z-[2] min-w-3 overflow-hidden rounded-md border text-left text-xs transition-colors focus-visible:outline-2 focus-visible:outline-primary"
                :class="barClass(slot)" :style="barStyle(slot)" :title="barTitle(row.aircraft, slot)"
                @click.stop="onBarClick(row.aircraft, slot)"
              >
                <span v-if="isActive(row.aircraft, slot)" class="absolute inset-y-0 left-0 bg-primary/45" :style="{ width: `${row.percent}%` }" />
                <span class="relative flex h-full items-center px-2">
                  <span class="truncate font-medium">{{ barLabel(row, slot) }}</span>
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- The same periods as a list: readable on small screens and with a screen reader -->
      <h2 class="mt-8 mb-3 text-lg font-semibold text-highlighted">{{ $t('maintenance.schedule.periods') }}</h2>
      <p v-if="!visibleSlots.length" class="rounded-xl border border-dashed border-default bg-default p-6 text-center text-muted">
        {{ $t('maintenance.schedule.empty') }}
      </p>
      <ul v-else class="divide-y divide-default overflow-hidden rounded-xl border border-default bg-default">
        <li v-for="slot in listedSlots" :key="slot.id" class="flex items-center gap-3 p-4">
          <span class="flex size-10 shrink-0 items-center justify-center rounded-lg" :class="isPast(slot) ? 'bg-elevated text-muted' : 'bg-primary/10 text-primary'">
            <UIcon name="i-lucide-calendar-range" class="size-5" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate font-medium text-highlighted">{{ aircraftLabel(slot.aircraftId) }}</p>
            <p class="text-sm text-muted">
              {{ formatPeriod(slot.startDate, slot.endDate) }} · {{ $t('maintenance.schedule.days', dayCount(slot)) }}
              <template v-if="slot.note"> · {{ slot.note }}</template>
            </p>
          </div>
          <UButton v-if="canManageMaintenance" color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="$t('maintenance.schedule.edit')" @click="openForm(slot)" />
        </li>
      </ul>
    </template>

    <MaintenanceWorkshopSlotForm v-model:open="formOpen" :slot="edited" :preset="preset" :aircraft="aircraft" @saved="onChanged" @deleted="onChanged" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { Aircraft, WorkshopSlot, WorkshopSlotInput } from '~/types/maintenance'

definePageMeta({
  middleware: ['authenticated']
})

const DAY_MS = 86_400_000
const DAY_WIDTH = 14 // px per day, a week is 98 px
const MIN_WEEKS = 8

const { t, locale } = useI18n()
const toast = useToast()
const { canManageMaintenance } = storeToRefs(useUserStore())
const { formatPeriod, progressPercent } = useMaintenance()

const aircraft = ref<Aircraft[]>([])
const slots = ref<WorkshopSlot[]>([])
const loaded = ref(false)
const errorMessage = ref<string | null>(null)
const showPast = ref(false)

// --- days as numbers (days since 1970-01-01 UTC), so time zones and daylight saving never shift a bar

const toDay = (iso: string) => {
  const [year, month, date] = iso.split('-').map(Number)
  return Date.UTC(year!, month! - 1, date!) / DAY_MS
}
const toIso = (day: number) => new Date(day * DAY_MS).toISOString().slice(0, 10)
const localToday = () => {
  const now = new Date()
  return Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) / DAY_MS
}
const mondayOf = (day: number) => day - ((new Date(day * DAY_MS).getUTCDay() + 6) % 7)
const isoWeek = (day: number) => {
  const date = new Date(day * DAY_MS)
  date.setUTCDate(date.getUTCDate() + 4 - (date.getUTCDay() || 7))
  const yearStart = Date.UTC(date.getUTCFullYear(), 0, 1)
  return Math.ceil(((date.getTime() - yearStart) / DAY_MS + 1) / 7)
}
const px = (days: number) => `${days * DAY_WIDTH}px`

const today = ref(localToday())
const isPast = (slot: WorkshopSlot) => toDay(slot.endDate) < today.value
const dayCount = (slot: WorkshopSlot) => toDay(slot.endDate) - toDay(slot.startDate) + 1

const visibleSlots = computed(() => slots.value.filter(slot => showPast.value || !isPast(slot)))
const listedSlots = computed(() => [...visibleSlots.value].sort((a, b) => a.startDate.localeCompare(b.startDate) || a.endDate.localeCompare(b.endDate)))

// The chart starts on the Monday of the earliest visible period (at the latest today) and shows at least MIN_WEEKS weeks.
const rangeStart = computed(() => mondayOf(Math.min(today.value, ...visibleSlots.value.map(slot => toDay(slot.startDate)))))
const rangeEnd = computed(() => {
  const last = Math.max(today.value, rangeStart.value + MIN_WEEKS * 7 - 1, ...visibleSlots.value.map(slot => toDay(slot.endDate)))
  return mondayOf(last) + 6
})
const totalDays = computed(() => rangeEnd.value - rangeStart.value + 1)
const todayOffset = computed(() => today.value - rangeStart.value)

const weeks = computed(() => Array.from({ length: totalDays.value / 7 }, (_, index) => ({
  offset: index * 7,
  number: isoWeek(rangeStart.value + index * 7)
})))

const months = computed(() => {
  const result: { key: string, label: string, days: number }[] = []
  for (let day = rangeStart.value; day <= rangeEnd.value; day++) {
    const iso = toIso(day)
    const key = iso.slice(0, 7)
    const last = result[result.length - 1]
    if (last?.key === key) {
      last.days++
    } else {
      const label = new Date(day * DAY_MS).toLocaleDateString(locale.value, { month: 'long', year: 'numeric', timeZone: 'UTC' })
      result.push({ key, label, days: 1 })
    }
  }
  return result
})

// Aircraft with planned periods first, ordered by their first period; the others follow so they can be planned too.
const rows = computed(() => aircraft.value
  .map((item) => {
    const own = visibleSlots.value.filter(slot => slot.aircraftId === item.id)
    return { aircraft: item, slots: own, first: own.length ? Math.min(...own.map(slot => toDay(slot.startDate))) : Infinity, percent: progressPercent(item) }
  })
  .sort((a, b) => a.first - b.first || a.aircraft.registration.localeCompare(b.aircraft.registration)))

// Tasks belong to the aircraft, not to a period: the progress is shown on the period running today or else the next one.
const isActive = (item: Aircraft, slot: WorkshopSlot) => item.workshop?.id === slot.id

const barStyle = (slot: WorkshopSlot) => ({
  left: px(toDay(slot.startDate) - rangeStart.value),
  width: px(dayCount(slot))
})
const barClass = (slot: WorkshopSlot) => isPast(slot)
  ? 'border-default bg-elevated text-muted hover:border-(--ui-border-accented)'
  : 'border-primary/50 bg-primary/15 text-highlighted hover:border-primary'
const barLabel = (row: { aircraft: Aircraft, percent: number }, slot: WorkshopSlot) => isActive(row.aircraft, slot)
  ? `${row.aircraft.registration} · ${row.percent} %`
  : slot.note || row.aircraft.registration
const barTitle = (item: Aircraft, slot: WorkshopSlot) => [
  `${item.registration} · ${item.type}`,
  formatPeriod(slot.startDate, slot.endDate),
  isActive(item, slot) ? t('maintenance.schedule.progress', { percent: progressPercent(item) }) : null,
  slot.note
].filter(Boolean).join('\n')

const aircraftLabel = (id: string) => {
  const item = aircraft.value.find(entry => entry.id === id)
  return item ? `${item.registration} · ${item.type}` : ''
}

// --- editing

const formOpen = ref(false)
const edited = ref<WorkshopSlot | null>(null)
const preset = ref<Partial<WorkshopSlotInput> | null>(null)

const openForm = (slot: WorkshopSlot | null, prefill: Partial<WorkshopSlotInput> | null = null) => {
  edited.value = slot
  preset.value = prefill
  formOpen.value = true
}

const onBarClick = (item: Aircraft, slot: WorkshopSlot) => canManageMaintenance.value
  ? openForm(slot)
  : navigateTo(`/maintenance/${item.id}`)

// A click into an empty part of a row plans the aircraft for a week from the clicked day.
const onTrackClick = (event: MouseEvent, item: Aircraft) => {
  if (!canManageMaintenance.value) {
    return
  }
  const start = rangeStart.value + Math.max(0, Math.floor(event.offsetX / DAY_WIDTH))
  openForm(null, { aircraftId: item.id, startDate: toIso(start), endDate: toIso(start + 6) })
}

const onChanged = async () => {
  toast.add({ title: t('maintenance.saved'), icon: 'i-lucide-circle-check', color: 'primary' })
  await fetchData()
}

const fetchData = async () => {
  const [aircraftResult, slotResult] = await Promise.all([
    useAPI<Aircraft[]>('/maintenance/aircraft', { method: 'GET' }),
    useAPI<WorkshopSlot[]>('/maintenance/workshop', { method: 'GET' })
  ])
  const failed = !!aircraftResult.error.value || !!slotResult.error.value
  errorMessage.value = failed ? t('maintenance.loadError') : null
  aircraft.value = aircraftResult.data.value ?? []
  slots.value = slotResult.data.value ?? []
  today.value = localToday()
  loaded.value = !failed
}

onMounted(fetchData)
</script>
