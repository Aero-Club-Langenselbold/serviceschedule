<template>
  <UModal v-model:open="open" :title="title" :ui="{ content: 'sm:max-w-2xl' }">
    <template #body>
      <form id="checklist-template-form" class="space-y-5" @submit.prevent="save">
        <UFormField :label="$t('operations.template.items')" :help="$t('operations.template.itemsHint')" required>
          <UTextarea v-model="items" :rows="6" autoresize class="w-full" :placeholder="$t('operations.template.itemsPlaceholder')" autofocus data-template-items />
        </UFormField>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="checklist-template-form" :label="$t('common.save')" :loading="saving" :disabled="!splitLines(items).length" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { ChecklistKind, ChecklistTemplate } from '~/types/operations'

/** Creates or replaces the start or end of operations checklist of an aircraft; one item per line. */
const props = defineProps<{ aircraftId: string, registration: string, kind: ChecklistKind, template?: ChecklistTemplate | null }>()
const emit = defineEmits<{ saved: [template: ChecklistTemplate] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { kindLabel } = useOperations()

const items = ref('')
const saving = ref(false)

const title = computed(() => t(props.template ? 'operations.template.edit' : 'operations.template.add',
  { kind: kindLabel(props.kind), registration: props.registration }))

watch(open, (isOpen) => {
  if (isOpen) {
    items.value = props.template?.items.join('\n') ?? ''
  }
}, { immediate: true })

const save = async () => {
  const lines = splitLines(items.value)
  if (!lines.length) {
    return
  }
  saving.value = true
  const { data, error } = await useAPI<ChecklistTemplate>(`/operations/aircraft/${props.aircraftId}/templates/${props.kind}`,
    { method: 'PUT', body: { items: lines } })
  saving.value = false

  if (error.value) {
    toast.add({ title: t('operations.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('saved', data.value as ChecklistTemplate)
}
</script>
