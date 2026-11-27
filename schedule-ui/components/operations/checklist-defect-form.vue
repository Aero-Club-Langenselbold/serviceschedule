<template>
  <UModal v-model:open="open" :title="$t('operations.defect.title')" :description="item?.text">
    <template #body>
      <form id="checklist-defect-form" class="space-y-5" @submit.prevent="save">
        <UFormField :label="$t('maintenance.defect.title')" required>
          <UInput v-model="form.title" :placeholder="$t('maintenance.defect.titlePlaceholder')" class="w-full" autofocus data-defect-title />
        </UFormField>
        <UFormField :label="$t('maintenance.priority.name')">
          <USelect v-model="form.priority" :items="priorityItems" class="w-full" />
        </UFormField>
        <UFormField :label="$t('maintenance.defect.description')">
          <UTextarea v-model="form.description" :rows="3" autoresize class="w-full" :placeholder="$t('maintenance.defect.descriptionPlaceholder')" />
        </UFormField>
        <p class="text-sm text-muted">{{ $t('operations.defect.hint') }}</p>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="checklist-defect-form" :label="$t('operations.defect.submit')" :loading="saving" :disabled="!form.title.trim()" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { DefectPriority } from '~/types/maintenance'
import type { ChecklistDetail, ChecklistPoint } from '~/types/operations'

/** Reports a defect to the defect book from a checklist item, which is then checked as not OK. */
const props = defineProps<{ checklistId: string, item: ChecklistPoint | null }>()
const emit = defineEmits<{ saved: [detail: ChecklistDetail] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { priorityItems } = useMaintenance()

const form = ref<{ title: string, description: string, priority: DefectPriority }>({ title: '', description: '', priority: 'NORMAL' })
const saving = ref(false)

watch(open, (isOpen) => {
  if (isOpen) {
    // the comment of the item is a good start for the description
    form.value = { title: '', description: props.item?.comment ?? '', priority: 'NORMAL' }
  }
}, { immediate: true })

const save = async () => {
  if (!props.item || !form.value.title.trim()) {
    return
  }
  saving.value = true
  const { data, error } = await useAPI<ChecklistDetail>(`/operations/checklists/${props.checklistId}/items/${props.item.id}/defect`,
    { method: 'POST', body: { ...form.value, assigneeIds: null } })
  saving.value = false

  if (error.value) {
    toast.add({ title: t('operations.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  toast.add({ title: t('operations.defect.reported'), icon: 'i-lucide-circle-check', color: 'primary' })
  open.value = false
  emit('saved', data.value as ChecklistDetail)
}
</script>
