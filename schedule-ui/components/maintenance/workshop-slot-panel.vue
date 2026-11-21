<template>
  <div class="w-[min(22rem,calc(100vw-2rem))] space-y-4 p-4" :data-slot-panel="slot.id">
    <div class="space-y-1">
      <p class="flex items-baseline justify-between gap-3">
        <span class="truncate font-semibold text-highlighted">{{ aircraft.registration }}</span>
        <span class="shrink-0 text-xs text-muted">{{ aircraft.type }}</span>
      </p>
      <p class="text-sm text-default">{{ formatPeriod(slot.startDate, slot.endDate) }}</p>
      <template v-if="slot.taskCount">
        <UProgress :model-value="slot.stepDoneCount" :max="Math.max(slot.stepCount, 1)" size="xs" :color="percent === 100 ? 'success' : 'primary'" />
        <p class="text-xs text-muted">
          {{ $t('maintenance.schedule.progress', { percent }) }} · {{ $t('maintenance.progress', { done: slot.doneCount, total: slot.taskCount }) }}
        </p>
      </template>
      <p v-else class="text-xs text-muted">{{ $t('maintenance.schedule.noTasks') }}</p>
      <p v-if="!canManage && slot.note" class="text-sm text-muted">{{ slot.note }}</p>
    </div>

    <!-- Maintenance administrators change the period right here -->
    <form v-if="canManage" :id="formId" class="space-y-3" @submit.prevent="save">
      <div class="grid grid-cols-2 gap-3">
        <UFormField :label="$t('maintenance.schedule.start')" size="sm">
          <UInput v-model="form.startDate" type="date" size="sm" class="w-full" />
        </UFormField>
        <UFormField :label="$t('maintenance.schedule.end')" size="sm">
          <UInput v-model="form.endDate" type="date" size="sm" :min="form.startDate || undefined" class="w-full" />
        </UFormField>
      </div>
      <p v-if="periodError" class="text-xs text-error">{{ periodError }}</p>
      <UFormField :label="$t('maintenance.schedule.note')" size="sm">
        <UInput v-model="form.note" size="sm" :placeholder="$t('maintenance.schedule.notePlaceholder')" maxlength="255" class="w-full" />
      </UFormField>
    </form>

    <div class="flex flex-wrap items-center gap-2 border-t border-default pt-3">
      <UButton :to="`/maintenance/${aircraft.id}?visit=${slot.id}`" color="neutral" variant="link" size="sm" trailing-icon="i-lucide-arrow-right" class="px-0" :label="$t('maintenance.schedule.openBoard')" />
      <template v-if="canManage">
        <template v-if="confirmDelete">
          <span class="ml-auto text-xs text-muted">{{ $t('maintenance.schedule.deleteAsk') }}</span>
          <UButton color="neutral" variant="ghost" size="sm" :label="$t('common.cancel')" @click="confirmDelete = false" />
          <UButton color="error" size="sm" :loading="deleting" :label="$t('common.delete')" data-confirm-delete @click="remove" />
        </template>
        <template v-else>
          <UButton color="error" variant="ghost" size="sm" icon="i-lucide-trash-2" class="ml-auto" :aria-label="$t('maintenance.schedule.delete')" @click="confirmDelete = true" />
          <UButton type="submit" :form="formId" size="sm" :label="$t('common.save')" :loading="saving" :disabled="!valid || !changed" />
        </template>
      </template>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { Aircraft, WorkshopSlot } from '~/types/maintenance'

const props = defineProps<{
  slot: WorkshopSlot
  aircraft: Aircraft
  canManage: boolean
}>()
const emit = defineEmits<{ saved: [slot: WorkshopSlot], deleted: [] }>()

const { t } = useI18n()
const toast = useToast()
const { formatPeriod, progressPercent } = useMaintenance()

const percent = computed(() => progressPercent(props.slot))

const formId = `workshop-slot-panel-${props.slot.id}`
const form = ref({ startDate: props.slot.startDate, endDate: props.slot.endDate, note: props.slot.note ?? '' })
const saving = ref(false)
const deleting = ref(false)
const overlap = ref(false)
const confirmDelete = ref(false)

watch(() => [form.value.startDate, form.value.endDate], () => { overlap.value = false })

// ISO dates compare correctly as strings
const periodInvalid = computed(() => !!form.value.startDate && !!form.value.endDate && form.value.endDate < form.value.startDate)
const periodError = computed(() => {
  if (periodInvalid.value) {
    return t('maintenance.schedule.invalidPeriod')
  }
  return overlap.value ? t('maintenance.schedule.overlap') : null
})
const valid = computed(() => !!form.value.startDate && !!form.value.endDate && !periodInvalid.value)
const changed = computed(() => form.value.startDate !== props.slot.startDate || form.value.endDate !== props.slot.endDate
  || form.value.note.trim() !== (props.slot.note ?? ''))

const save = async () => {
  if (!valid.value || !changed.value) {
    return
  }
  saving.value = true
  const body = { aircraftId: props.slot.aircraftId, startDate: form.value.startDate, endDate: form.value.endDate, note: form.value.note.trim() || null }
  const { data, error } = await useAPI<WorkshopSlot>(`/maintenance/workshop/${props.slot.id}`, { method: 'PUT', body })
  saving.value = false

  if (error.value) {
    if (error.value.statusCode === 409) {
      overlap.value = true
    } else {
      toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    }
    return
  }
  emit('saved', data.value as WorkshopSlot)
}

const remove = async () => {
  deleting.value = true
  const { error } = await useAPI(`/maintenance/workshop/${props.slot.id}`, { method: 'DELETE' })
  deleting.value = false
  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  emit('deleted')
}
</script>
