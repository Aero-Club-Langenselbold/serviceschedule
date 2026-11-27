<template>
  <AppPage
    :title="title" :description="today ? `${today.registration} · ${today.aircraftType}` : undefined" :error-message="errorMessage"
    back="/operations" :back-label="$t('operations.overview.title')" width="md"
  >
    <template v-if="today" #actions>
      <UButton
        v-if="canManageChecklists" :to="`/operations/print/${today.aircraftId}/${today.kind.toLowerCase()}`" target="_blank"
        color="neutral" variant="outline" icon="i-lucide-qr-code" :label="$t('operations.print.open')"
      />
    </template>

    <template v-if="today">
      <OperationsChecklistView v-if="today.checklist" :detail="today.checklist" @update="today.checklist = $event" />
      <div v-else-if="today.templateItems" class="rounded-xl border border-default bg-default p-4" data-start>
        <p class="font-medium text-highlighted">{{ $t('operations.today.notStarted', { date: formatDay(today.date) }) }}</p>
        <p class="mt-1 text-sm text-muted">{{ $t('operations.today.startHint') }}</p>
        <ol class="mt-4 list-decimal space-y-1 pl-5 text-sm text-default">
          <li v-for="(item, index) in today.templateItems" :key="index">{{ item }}</li>
        </ol>
        <UButton class="mt-4" icon="i-lucide-play" :label="$t('operations.today.start')" :loading="starting" data-start-button @click="start" />
      </div>
      <p v-else class="text-sm text-muted" data-no-template>{{ $t('operations.today.noTemplate') }}</p>
    </template>
  </AppPage>
</template>

<script lang="ts" setup>
import type { ChecklistDetail, TodayChecklist } from '~/types/operations'

/** Target of the QR code: always the checklist of the current day of an aircraft and kind. */
const route = useRoute()
const { t } = useI18n()
const { kindLabel } = useOperations()
const { formatDay } = useMaintenance()
const { canManageChecklists } = storeToRefs(useUserStore())

const kind = computed(() => parseKind(route.params.kind))
const today = ref<TodayChecklist | null>(null)
const errorMessage = ref<string | null>(null)
const starting = ref(false)

const title = computed(() => kind.value ? kindLabel(kind.value) : t('operations.overview.title'))

const load = async () => {
  if (!kind.value) {
    errorMessage.value = t('operations.notFound')
    return
  }
  const { data, error } = await useAPI<TodayChecklist>(`/operations/aircraft/${route.params.aircraftId}/checklists/${kind.value}/today`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = error.value.statusCode === 404 ? t('operations.notFound') : t('operations.loadError')
    return
  }
  errorMessage.value = null
  today.value = data.value as TodayChecklist
}

const start = async () => {
  starting.value = true
  const { data, error } = await useAPI<ChecklistDetail>(`/operations/aircraft/${route.params.aircraftId}/checklists/${kind.value}/today`, { method: 'POST' })
  starting.value = false
  if (error.value) {
    // started by somebody else in the meantime: show theirs
    if (error.value.statusCode === 409) {
      await load()
      return
    }
    errorMessage.value = t('operations.saveError')
    return
  }
  today.value!.checklist = data.value as ChecklistDetail
}

watch(() => route.fullPath, load)
onMounted(load)

definePageMeta({
  middleware: ['authenticated']
})
</script>
