<template>
  <UModal v-model:open="open" :title="template ? $t('maintenance.templates.edit') : $t('maintenance.templates.add')" :ui="{ content: 'sm:max-w-2xl' }">
    <template #body>
      <form id="template-form" class="space-y-5" @submit.prevent="save">
        <UFormField :label="$t('maintenance.task.title')" required>
          <UInput v-model="form.title" :placeholder="$t('maintenance.task.titlePlaceholder')" class="w-full" autofocus />
        </UFormField>
        <UFormField :label="$t('maintenance.level.name')">
          <URadioGroup v-model="form.level" orientation="horizontal" variant="table" :items="levelItems" />
        </UFormField>
        <div class="grid gap-4 sm:grid-cols-2">
          <UFormField :label="$t('maintenance.duration.name')">
            <USelect v-model="duration" :items="durationItems" class="w-full" />
          </UFormField>
          <UFormField :label="$t('maintenance.templates.aircraftTypes')" :hint="form.aircraftTypes.length ? undefined : $t('maintenance.templates.allAircraft')">
            <USelectMenu v-model="form.aircraftTypes" :items="typeItems" multiple create-item class="w-full" :placeholder="$t('maintenance.templates.allAircraft')" @create="addType" />
          </UFormField>
        </div>
        <p class="-mt-3 text-xs text-muted">{{ $t('maintenance.templates.aircraftTypesHint') }}</p>
        <UFormField :label="$t('maintenance.task.description')">
          <UTextarea v-model="description" :rows="2" autoresize class="w-full" :placeholder="$t('maintenance.task.descriptionPlaceholder')" />
        </UFormField>
        <UFormField :label="$t('maintenance.task.checklist')" :help="$t('maintenance.task.checklistHint')">
          <UTextarea v-model="checklist" :rows="3" autoresize class="w-full" />
        </UFormField>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="template-form" :label="$t('common.save')" :loading="saving" :disabled="!form.title.trim()" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { TaskTemplate, TaskTemplateInput } from '~/types/maintenance'

const props = defineProps<{ template?: TaskTemplate | null, aircraftTypes: string[] }>()
const emit = defineEmits<{ saved: [template: TaskTemplate] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { durationItems, levelItems } = useMaintenance()

const empty = (): TaskTemplateInput => ({ title: '', description: null, level: 'BEGINNER', durationMinutes: null, aircraftTypes: [], checklist: [] })
const form = ref<TaskTemplateInput>(empty())
const description = ref('')
const checklist = ref('')
const createdTypes = ref<string[]>([])
const saving = ref(false)
const duration = computed({
  get: () => toDuration(form.value.durationMinutes),
  set: (minutes: number) => { form.value.durationMinutes = fromDuration(minutes) }
})

const typeItems = computed(() => [...new Set([...props.aircraftTypes, ...createdTypes.value, ...form.value.aircraftTypes])].sort())

watch(open, (isOpen) => {
  if (isOpen) {
    form.value = props.template ? { ...props.template, aircraftTypes: [...props.template.aircraftTypes] } : empty()
    description.value = props.template?.description ?? ''
    checklist.value = (props.template?.checklist ?? []).join('\n')
    createdTypes.value = []
  }
}, { immediate: true })

const addType = (type: string) => {
  const trimmed = type.trim()
  if (trimmed && !form.value.aircraftTypes.includes(trimmed)) {
    createdTypes.value.push(trimmed)
    form.value.aircraftTypes.push(trimmed)
  }
}

const save = async () => {
  if (!form.value.title.trim()) {
    return
  }
  saving.value = true
  const body = { ...form.value, description: description.value, checklist: splitLines(checklist.value) }
  const { data, error } = props.template
    ? await useAPI<TaskTemplate>(`/maintenance/templates/${props.template.id}`, { method: 'PUT', body })
    : await useAPI<TaskTemplate>('/maintenance/templates', { method: 'POST', body })
  saving.value = false

  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('saved', data.value as TaskTemplate)
}
</script>
