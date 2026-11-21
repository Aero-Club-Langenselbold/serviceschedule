<template>
  <UModal v-model:open="open" :title="slot ? $t('maintenance.schedule.edit') : $t('maintenance.schedule.add')">
    <template #body>
      <form id="workshop-slot-form" class="space-y-5" @submit.prevent="save">
        <!-- the tasks of a visit belong to its aircraft -->
        <UFormField :label="$t('maintenance.schedule.aircraft')" required :help="slot?.taskCount ? $t('maintenance.schedule.aircraftLocked') : undefined">
          <USelect v-model="form.aircraftId" :items="aircraftItems" :disabled="!!slot?.taskCount" :placeholder="$t('maintenance.schedule.chooseAircraft')" class="w-full" />
        </UFormField>
        <div class="grid gap-4 sm:grid-cols-2">
          <UFormField :label="$t('maintenance.schedule.start')" required>
            <UInput v-model="form.startDate" type="date" class="w-full" />
          </UFormField>
          <UFormField :label="$t('maintenance.schedule.end')" required :error="periodError ?? undefined">
            <UInput v-model="form.endDate" type="date" :min="form.startDate || undefined" class="w-full" />
          </UFormField>
        </div>
        <UFormField :label="$t('maintenance.schedule.note')">
          <UInput v-model="note" :placeholder="$t('maintenance.schedule.notePlaceholder')" maxlength="255" class="w-full" />
        </UFormField>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:items-center">
        <ConfirmationDialog
          v-if="slot" variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="$t('maintenance.schedule.delete')" button-class="sm:mr-auto"
          :tooltip="$t('maintenance.schedule.delete')" :modal-title="$t('maintenance.schedule.deleteConfirm')"
          :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="remove"
        />
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" class="sm:ml-auto" @click="open = false" />
        <UButton type="submit" form="workshop-slot-form" :label="$t('common.save')" :loading="saving" :disabled="!valid" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Aircraft, WorkshopSlot, WorkshopSlotInput } from '~/types/maintenance'

const props = defineProps<{
  slot?: WorkshopSlot | null
  aircraft: Aircraft[]
  /** Prefill for a new period, e.g. from a click into the chart. */
  preset?: Partial<WorkshopSlotInput> | null
}>()
const emit = defineEmits<{ saved: [slot: WorkshopSlot], deleted: [] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()

const form = ref<WorkshopSlotInput>({ aircraftId: '', startDate: '', endDate: '', note: null })
const note = ref('')
const saving = ref(false)
const overlap = ref(false)

const aircraftItems = computed(() => props.aircraft.map(item => ({ label: `${item.registration} · ${item.type}`, value: item.id })))

watch(open, (isOpen) => {
  if (isOpen) {
    const source = props.slot ?? props.preset ?? {}
    form.value = {
      aircraftId: source.aircraftId ?? '',
      startDate: source.startDate ?? '',
      endDate: source.endDate ?? '',
      note: null
    }
    note.value = source.note ?? ''
    overlap.value = false
  }
}, { immediate: true })

watch(() => [form.value.aircraftId, form.value.startDate, form.value.endDate], () => { overlap.value = false })

// ISO dates compare correctly as strings
const periodInvalid = computed(() => !!form.value.startDate && !!form.value.endDate && form.value.endDate < form.value.startDate)
const periodError = computed(() => {
  if (periodInvalid.value) {
    return t('maintenance.schedule.invalidPeriod')
  }
  return overlap.value ? t('maintenance.schedule.overlap') : null
})
const valid = computed(() => !!form.value.aircraftId && !!form.value.startDate && !!form.value.endDate && !periodInvalid.value)

const save = async () => {
  if (!valid.value) {
    return
  }
  saving.value = true
  const body = { ...form.value, note: note.value.trim() || null }
  const { data, error } = props.slot
    ? await useAPI<WorkshopSlot>(`/maintenance/workshop/${props.slot.id}`, { method: 'PUT', body })
    : await useAPI<WorkshopSlot>('/maintenance/workshop', { method: 'POST', body })
  saving.value = false

  if (error.value) {
    if (error.value.statusCode === 409) {
      overlap.value = true
    } else {
      toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    }
    return
  }
  open.value = false
  emit('saved', data.value as WorkshopSlot)
}

const remove = async () => {
  if (!props.slot) {
    return
  }
  const { error } = await useAPI(`/maintenance/workshop/${props.slot.id}`, { method: 'DELETE' })
  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('deleted')
}
</script>
