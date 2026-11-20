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
      <li v-for="item in aircraft" :key="item.id">
        <NuxtLink :to="`/maintenance/${item.id}`" class="flex h-full flex-col gap-4 rounded-xl border border-default bg-default p-4 transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary">
          <div class="flex items-start gap-3">
            <span class="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <UIcon name="i-lucide-plane" class="size-5" />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block truncate font-semibold text-highlighted">{{ item.registration }}</span>
              <span class="block truncate text-sm text-muted">{{ item.type }}</span>
            </span>
            <UIcon name="i-lucide-chevron-right" class="mt-1 size-4 shrink-0 text-dimmed" />
          </div>

          <p class="flex items-center gap-1.5 text-sm text-muted">
            <UIcon name="i-lucide-calendar-range" class="size-4 shrink-0" />
            <span v-if="item.workshop">{{ formatPeriod(item.workshop.startDate, item.workshop.endDate) }}</span>
            <span v-else>{{ $t('maintenance.noPeriod') }}</span>
          </p>

          <div class="mt-auto space-y-2">
            <div class="flex items-baseline justify-between gap-2 text-sm">
              <span class="text-default">{{ $t('maintenance.progress', { done: item.doneCount, total: item.taskCount }) }}</span>
              <span v-if="item.signedCount" class="text-xs text-success">{{ $t('maintenance.signedProgress', { count: item.signedCount }) }}</span>
            </div>
            <UProgress
              :model-value="item.stepDoneCount" :max="Math.max(item.stepCount, 1)" size="sm"
              :color="item.stepCount && item.stepDoneCount === item.stepCount ? 'success' : 'primary'"
              :aria-label="$t('maintenance.schedule.progress', { percent: progressPercent(item) })"
            />
            <UBadge v-if="item.openDefectCount" color="warning" variant="subtle" icon="i-lucide-triangle-alert">
              {{ $t('maintenance.openDefects', item.openDefectCount) }}
            </UBadge>
          </div>
        </NuxtLink>
      </li>
    </ul>

    <MaintenanceAircraftForm v-model:open="formOpen" @saved="onSaved" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { Aircraft } from '~/types/maintenance'

definePageMeta({
  middleware: ['authenticated']
})

const { t } = useI18n()
const { canManageMaintenance } = storeToRefs(useUserStore())
const { formatPeriod, progressPercent } = useMaintenance()

const aircraft = ref<Aircraft[]>([])
const loaded = ref(false)
const errorMessage = ref<string | null>(null)
const formOpen = ref(false)

const fetchData = async () => {
  const { data, error } = await useAPI<Aircraft[]>('/maintenance/aircraft', { method: 'GET' })
  errorMessage.value = error.value ? t('maintenance.loadError') : null
  aircraft.value = data.value ?? []
  loaded.value = !error.value
}

const onSaved = (created: Aircraft) => navigateTo(`/maintenance/${created.id}`)

onMounted(fetchData)
</script>
