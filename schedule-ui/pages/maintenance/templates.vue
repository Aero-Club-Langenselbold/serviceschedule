<template>
  <AppPage :title="$t('maintenance.templates.title')" :description="$t('maintenance.templates.description')" back="/maintenance" :back-label="$t('nav.maintenance.overview')" :error-message="errorMessage" width="md">
    <template #actions>
      <UButton icon="i-lucide-plus" :label="$t('maintenance.templates.add')" @click="openForm(null)" />
    </template>

    <p v-if="loaded && !templates.length" class="rounded-xl border border-dashed border-default bg-default p-6 text-center text-muted">{{ $t('maintenance.templates.empty') }}</p>
    <ul v-else class="divide-y divide-default overflow-hidden rounded-xl border border-default bg-default">
      <li v-for="template in templates" :key="template.id" class="flex items-start gap-3 p-4">
        <div class="min-w-0 flex-1 space-y-1.5">
          <div class="flex flex-wrap items-center gap-2">
            <span class="font-medium text-highlighted">{{ template.title }}</span>
            <UBadge :color="levelColor(template.level)" variant="outline" size="sm">{{ levelLabel(template.level) }}</UBadge>
          </div>
          <p v-if="template.description" class="line-clamp-2 text-sm text-muted">{{ template.description }}</p>
          <p class="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted">
            <span class="inline-flex items-center gap-1"><UIcon name="i-lucide-plane" class="size-3.5" />{{ template.aircraftTypes.length ? template.aircraftTypes.join(', ') : $t('maintenance.templates.allAircraft') }}</span>
            <span v-if="template.durationMinutes" class="inline-flex items-center gap-1"><UIcon name="i-lucide-clock" class="size-3.5" />{{ formatDuration(template.durationMinutes) }}</span>
            <span class="inline-flex items-center gap-1"><UIcon name="i-lucide-list-checks" class="size-3.5" />{{ $t('maintenance.templates.steps', template.checklist.length) }}</span>
          </p>
        </div>
        <div class="flex shrink-0 gap-1">
          <UButton color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="$t('maintenance.templates.edit')" @click="openForm(template)" />
          <ConfirmationDialog
            variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
            :tooltip="$t('maintenance.templates.delete')" :modal-title="$t('maintenance.templates.deleteConfirm')"
            :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="() => remove(template)"
          />
        </div>
      </li>
    </ul>

    <MaintenanceTemplateForm v-model:open="formOpen" :template="edited" :aircraft-types="aircraftTypes" @saved="onSaved" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { TaskTemplate } from '~/types/maintenance'

definePageMeta({
  middleware: ['authenticated', 'maintenance-admin']
})

const { t } = useI18n()
const toast = useToast()
const { levelLabel, levelColor, formatDuration } = useMaintenance()

const templates = ref<TaskTemplate[]>([])
const aircraftTypes = ref<string[]>([])
const loaded = ref(false)
const errorMessage = ref<string | null>(null)
const formOpen = ref(false)
const edited = ref<TaskTemplate | null>(null)

const fetchData = async () => {
  const [templateResult, typeResult] = await Promise.all([
    useAPI<TaskTemplate[]>('/maintenance/templates', { method: 'GET' }),
    useAPI<string[]>('/maintenance/aircraft/types', { method: 'GET' })
  ])
  errorMessage.value = templateResult.error.value ? t('maintenance.loadError') : null
  templates.value = templateResult.data.value ?? []
  aircraftTypes.value = typeResult.data.value ?? []
  loaded.value = !templateResult.error.value
}

const openForm = (template: TaskTemplate | null) => {
  edited.value = template
  formOpen.value = true
}

const onSaved = async () => {
  toast.add({ title: t('maintenance.saved'), icon: 'i-lucide-circle-check', color: 'primary' })
  await fetchData()
}

const remove = async (template: TaskTemplate) => {
  const { error } = await useAPI(`/maintenance/templates/${template.id}`, { method: 'DELETE' })
  toast.add(error.value
    ? { title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' }
    : { title: t('maintenance.deleted'), icon: 'i-lucide-circle-check', color: 'primary' })
  await fetchData()
}

onMounted(fetchData)
</script>
