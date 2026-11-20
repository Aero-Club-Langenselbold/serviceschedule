<template>
  <UModal v-model:open="open" :title="defect ? $t('maintenance.defect.edit') : $t('maintenance.defect.add')">
    <template #body>
      <form id="defect-form" class="space-y-5" @submit.prevent="save">
        <UFormField :label="$t('maintenance.defect.title')" required>
          <UInput v-model="form.title" :placeholder="$t('maintenance.defect.titlePlaceholder')" class="w-full" autofocus />
        </UFormField>
        <UFormField :label="$t('maintenance.priority.name')">
          <USelect v-model="form.priority" :items="priorityItems" class="w-full" />
        </UFormField>
        <UFormField :label="$t('maintenance.defect.description')">
          <UTextarea v-model="description" :rows="3" autoresize class="w-full" :placeholder="$t('maintenance.defect.descriptionPlaceholder')" />
        </UFormField>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="defect-form" :label="$t('common.save')" :loading="saving" :disabled="!form.title.trim()" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Defect, DefectInput } from '~/types/maintenance'

const props = defineProps<{ aircraftId: string, defect?: Defect | null }>()
const emit = defineEmits<{ saved: [defect: Defect] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { priorityItems } = useMaintenance()

const form = ref<DefectInput>({ title: '', description: null, priority: 'NORMAL' })
const description = ref('')
const saving = ref(false)

watch(open, (isOpen) => {
  if (isOpen) {
    form.value = { title: props.defect?.title ?? '', description: null, priority: props.defect?.priority ?? 'NORMAL' }
    description.value = props.defect?.description ?? ''
  }
}, { immediate: true })

const save = async () => {
  if (!form.value.title.trim()) {
    return
  }
  saving.value = true
  const body = { ...form.value, description: description.value }
  const { data, error } = props.defect
    ? await useAPI<Defect>(`/maintenance/defects/${props.defect.id}`, { method: 'PUT', body })
    : await useAPI<Defect>(`/maintenance/aircraft/${props.aircraftId}/defects`, { method: 'POST', body })
  saving.value = false

  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('saved', data.value as Defect)
}
</script>
