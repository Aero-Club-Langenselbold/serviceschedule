<template>
  <AppPage
    :title="title" :description="detail ? `${detail.checklist.registration} · ${detail.checklist.aircraftType}` : undefined"
    :error-message="errorMessage" :back="canManageOperations ? '/operations/archive' : '/operations'"
    :back-label="canManageOperations ? $t('operations.archive.title') : $t('operations.overview.title')" width="md"
  >
    <OperationsChecklistView v-if="detail" :detail="detail" @update="detail = $event" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { ChecklistDetail } from '~/types/operations'

/** A checklist by id: the archive and the notifications lead here. Members only see the checklists of today. */
const route = useRoute()
const { t } = useI18n()
const { kindLabel } = useOperations()
const { canManageOperations } = storeToRefs(useUserStore())

const detail = ref<ChecklistDetail | null>(null)
const errorMessage = ref<string | null>(null)

const title = computed(() => detail.value ? kindLabel(detail.value.checklist.kind) : t('operations.overview.title'))

const load = async () => {
  const { data, error } = await useAPI<ChecklistDetail>(`/operations/checklists/${route.params.id}`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = error.value.statusCode === 404 ? t('operations.notFound')
      : error.value.statusCode === 403 ? t('operations.forbidden') : t('operations.loadError')
    return
  }
  errorMessage.value = null
  detail.value = data.value as ChecklistDetail
}

watch(() => route.params.id, load)
onMounted(load)

definePageMeta({
  middleware: ['authenticated']
})
</script>
