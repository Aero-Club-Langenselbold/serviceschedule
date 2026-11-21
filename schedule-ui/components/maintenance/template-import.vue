<template>
  <UModal v-model:open="open" :title="$t('maintenance.board.importTemplates')" :description="boardLabel" :ui="{ content: 'sm:max-w-2xl' }">
    <template #body>
      <div v-if="loading" class="flex justify-center py-6"><UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-muted" /></div>
      <p v-else-if="!templates.length" class="rounded-xl border border-dashed border-default p-6 text-center text-muted">{{ $t('maintenance.import.empty') }}</p>
      <div v-else class="space-y-3">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <p class="text-sm text-muted">{{ $t('maintenance.import.hint') }}</p>
          <UButton
            color="neutral" variant="link" size="sm" class="px-0" :disabled="!available.length"
            :label="allChosen ? $t('maintenance.import.none') : $t('maintenance.import.all')" @click="toggleAll"
          />
        </div>
        <ul class="divide-y divide-default overflow-hidden rounded-xl border border-default">
          <li v-for="template in templates" :key="template.id" :data-template="template.id">
            <label
              class="flex items-start gap-3 p-3"
              :class="isExisting(template) ? 'cursor-not-allowed opacity-60' : 'cursor-pointer hover:bg-elevated/50'"
            >
              <UCheckbox
                :model-value="isExisting(template) || chosen.has(template.id)" :disabled="isExisting(template)" class="mt-0.5"
                :aria-label="template.title" @update:model-value="(value: boolean | 'indeterminate') => toggle(template.id, value === true)"
              />
              <span class="min-w-0 flex-1 space-y-1">
                <span class="flex flex-wrap items-center gap-2">
                  <span class="font-medium text-highlighted">{{ template.title }}</span>
                  <UBadge :color="levelColor(template.level)" variant="outline" size="sm">{{ levelLabel(template.level) }}</UBadge>
                </span>
                <span v-if="template.description" class="line-clamp-2 block text-sm text-muted">{{ template.description }}</span>
                <span class="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted">
                  <span v-if="template.durationMinutes" class="inline-flex items-center gap-1"><UIcon name="i-lucide-clock" class="size-3.5" />{{ formatDuration(template.durationMinutes) }}</span>
                  <span v-if="template.checklist.length" class="inline-flex items-center gap-1"><UIcon name="i-lucide-list-checks" class="size-3.5" />{{ $t('maintenance.import.steps', template.checklist.length) }}</span>
                  <span v-if="isExisting(template)" class="inline-flex items-center gap-1 font-medium text-success"><UIcon name="i-lucide-check" class="size-3.5" />{{ $t('maintenance.import.existing') }}</span>
                </span>
              </span>
            </label>
          </li>
        </ul>
      </div>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton
          icon="i-lucide-copy-plus" :label="$t('maintenance.import.create', chosen.size)" :loading="saving" :disabled="!chosen.size"
          data-import-submit @click="save"
        />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Aircraft, TaskTemplate } from '~/types/maintenance'

/** Lets maintenance administrators choose which templates become tasks on the board of a workshop visit. */
const props = defineProps<{
  aircraft: Aircraft
  /** The visit whose board gets the tasks, null for the board without period. */
  workshopId: string | null
  boardLabel: string
  /** Templates that already have a task on that board. */
  existing: string[]
}>()
const emit = defineEmits<{ imported: [created: number] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { levelLabel, levelColor, formatDuration } = useMaintenance()

const templates = ref<TaskTemplate[]>([])
const chosen = ref(new Set<string>())
const loading = ref(false)
const saving = ref(false)

// Templates without aircraft types apply to every aircraft.
const applies = (template: TaskTemplate) => !template.aircraftTypes.length
  || template.aircraftTypes.some(type => type.toLowerCase() === props.aircraft.type.toLowerCase())
const isExisting = (template: TaskTemplate) => props.existing.includes(template.id)
const available = computed(() => templates.value.filter(template => !isExisting(template)))
const allChosen = computed(() => available.value.length > 0 && available.value.every(template => chosen.value.has(template.id)))

const toggle = (id: string, value: boolean) => {
  const next = new Set(chosen.value)
  if (value) {
    next.add(id)
  } else {
    next.delete(id)
  }
  chosen.value = next
}
const toggleAll = () => {
  chosen.value = allChosen.value ? new Set() : new Set(available.value.map(template => template.id))
}

// Nothing is preselected: not every visit does all the work.
watch(open, async (isOpen) => {
  if (!isOpen) {
    return
  }
  chosen.value = new Set()
  loading.value = true
  const { data, error } = await useAPI<TaskTemplate[]>('/maintenance/templates', { method: 'GET' })
  loading.value = false
  if (error.value) {
    toast.add({ title: t('maintenance.loadError'), icon: 'i-lucide-bomb', color: 'error' })
  }
  templates.value = (data.value ?? []).filter(applies)
}, { immediate: true })

const save = async () => {
  if (!chosen.value.size) {
    return
  }
  saving.value = true
  const body = { workshopId: props.workshopId, templateIds: [...chosen.value] }
  const { data, error } = await useAPI<{ created: number }>(`/maintenance/aircraft/${props.aircraft.id}/import-templates`, { method: 'POST', body })
  saving.value = false
  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('imported', (data.value as { created: number }).created)
}
</script>
