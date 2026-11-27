<template>
  <UModal v-model:open="open" :title="task ? $t('maintenance.task.edit') : $t('maintenance.task.add')" :description="aircraftLabel" :ui="{ content: 'sm:max-w-2xl' }">
    <template #body>
      <form id="task-form" class="space-y-5" @submit.prevent="save">
        <UFormField :label="$t('maintenance.task.title')" required>
          <UInput v-model="form.title" :placeholder="$t('maintenance.task.titlePlaceholder')" class="w-full" autofocus />
        </UFormField>
        <UFormField v-if="workshops.length" :label="$t('maintenance.board.visit')">
          <USelect v-model="workshopId" :items="workshopItems" class="w-full" />
        </UFormField>
        <UFormField :label="$t('maintenance.level.name')">
          <URadioGroup v-model="form.level" orientation="horizontal" variant="table" :items="levelItems" />
        </UFormField>
        <div class="grid gap-4 sm:grid-cols-2">
          <UFormField :label="$t('maintenance.duration.name')">
            <USelect v-model="duration" :items="durationItems" class="w-full" />
          </UFormField>
          <UFormField :label="$t('maintenance.task.deadline')">
            <UInput v-model="deadline" type="date" class="w-full" />
          </UFormField>
        </div>
        <UFormField :label="$t('maintenance.task.description')">
          <UTextarea v-model="description" :rows="2" autoresize class="w-full" :placeholder="$t('maintenance.task.descriptionPlaceholder')" />
        </UFormField>
        <UFormField :label="$t('maintenance.task.checklist')" :help="$t('maintenance.task.checklistHint')">
          <UTextarea v-model="checklist" :rows="3" autoresize class="w-full" />
        </UFormField>
        <UFormField v-if="defectItems.length > 1" :label="$t('maintenance.task.defect')">
          <USelect v-model="defectId" :items="defectItems" class="w-full" />
        </UFormField>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="task-form" :label="$t('common.save')" :loading="saving" :disabled="!form.title.trim()" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Defect, TaskDetail, TaskInput, WorkshopSlot } from '~/types/maintenance'
import { NO_SELECTION } from '~/composables/useMaintenance'

/** Creates a task or (with task) edits one; with defect, prefilled from a defect/finding. */
const props = defineProps<{
  aircraftId: string
  aircraftLabel?: string
  task?: TaskDetail | null
  defect?: Defect | null
  defects: Defect[]
  /** Workshop visits of the aircraft; every visit has its own board. */
  workshops: WorkshopSlot[]
  /** Visit of a new task: the board it was added on, null for the board without period. */
  workshopId: string | null
}>()
const emit = defineEmits<{ saved: [task: TaskDetail] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { durationItems, levelItems, formatPeriod } = useMaintenance()

const empty = (): TaskInput => ({ title: '', description: null, level: 'BEGINNER', durationMinutes: null, deadline: null, defectId: null, workshopId: props.workshopId, checklist: [] })
const form = ref<TaskInput>(empty())
const description = ref('')
const checklist = ref('')
const deadline = ref('')
const saving = ref(false)
const defectId = computed({
  get: () => form.value.defectId ?? NO_SELECTION,
  set: (id: string) => { form.value.defectId = id === NO_SELECTION ? null : id }
})
const workshopId = computed({
  get: () => form.value.workshopId ?? NO_SELECTION,
  set: (id: string) => { form.value.workshopId = id === NO_SELECTION ? null : id }
})
const workshopItems = computed(() => [
  ...[...props.workshops].reverse().map(slot => ({ label: [formatPeriod(slot.startDate, slot.endDate), slot.note].filter(Boolean).join(' · '), value: slot.id })),
  { label: t('maintenance.board.unscheduled'), value: NO_SELECTION }
])
const duration = computed({
  get: () => toDuration(form.value.durationMinutes),
  set: (minutes: number) => { form.value.durationMinutes = fromDuration(minutes) }
})

watch(open, (isOpen) => {
  if (!isOpen) {
    return
  }
  const task = props.task
  if (task) {
    form.value = { title: task.title, description: task.description, level: task.level, durationMinutes: task.durationMinutes, deadline: task.deadline, defectId: task.defect?.id ?? null, workshopId: task.workshopId, checklist: [] }
  } else if (props.defect) {
    form.value = { ...empty(), title: props.defect.title, level: 'EXPERIENCED', defectId: props.defect.id }
  } else {
    form.value = empty()
  }
  description.value = task?.description ?? props.defect?.description ?? ''
  checklist.value = (task?.checklist ?? []).map(item => item.text).join('\n')
  deadline.value = task?.deadline ?? ''
}, { immediate: true })

// Open defects without a task, plus the one this task already resolves.
const defectItems = computed(() => [
  { label: t('maintenance.task.noDefect'), value: NO_SELECTION },
  ...props.defects
    .filter(defect => defect.id === form.value.defectId || ((defect.status === 'OPEN' || defect.status === 'IN_PROGRESS') && !defect.signedAt && !defect.task))
    .map(defect => ({ label: defect.title, value: defect.id }))
])

const save = async () => {
  if (!form.value.title.trim()) {
    return
  }
  saving.value = true
  const body = { ...form.value, description: description.value, deadline: deadline.value || null, checklist: splitLines(checklist.value) }
  const { data, error } = props.task
    ? await useAPI<TaskDetail>(`/maintenance/tasks/${props.task.id}`, { method: 'PUT', body })
    : await useAPI<TaskDetail>(`/maintenance/aircraft/${props.aircraftId}/tasks`, { method: 'POST', body })
  saving.value = false

  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('saved', data.value as TaskDetail)
}
</script>
