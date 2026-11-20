<template>
  <UModal v-model:open="open" :title="aircraft ? $t('maintenance.aircraft.edit') : $t('maintenance.aircraft.add')">
    <template #body>
      <form id="aircraft-form" class="space-y-5" @submit.prevent="save">
        <div class="grid gap-4 sm:grid-cols-2">
          <UFormField :label="$t('maintenance.aircraft.type')" required>
            <UInput v-model="form.type" :placeholder="$t('maintenance.aircraft.typePlaceholder')" class="w-full" autofocus />
          </UFormField>
          <UFormField :label="$t('maintenance.aircraft.registration')" required :error="registrationError ?? undefined">
            <UInput v-model="form.registration" :placeholder="$t('maintenance.aircraft.registrationPlaceholder')" class="w-full" />
          </UFormField>
          <UFormField :label="$t('maintenance.aircraft.workshopStart')">
            <UInput v-model="form.workshopStart" type="date" class="w-full" />
          </UFormField>
          <UFormField :label="$t('maintenance.aircraft.workshopEnd')" :error="periodInvalid ? $t('maintenance.aircraft.invalidPeriod') : undefined">
            <UInput v-model="form.workshopEnd" type="date" :min="form.workshopStart ?? undefined" class="w-full" />
          </UFormField>
        </div>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="aircraft-form" :label="$t('common.save')" :loading="saving" :disabled="!valid" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Aircraft, AircraftInput } from '~/types/maintenance'

const props = defineProps<{ aircraft?: Aircraft | null }>()
const emit = defineEmits<{ saved: [aircraft: Aircraft] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()

const empty = (): AircraftInput => ({ type: '', registration: '', workshopStart: null, workshopEnd: null })
const form = ref<AircraftInput>(empty())
const saving = ref(false)
const registrationError = ref<string | null>(null)

watch(open, (isOpen) => {
  if (isOpen) {
    form.value = props.aircraft
      ? { type: props.aircraft.type, registration: props.aircraft.registration, workshopStart: props.aircraft.workshopStart, workshopEnd: props.aircraft.workshopEnd }
      : empty()
    registrationError.value = null
  }
}, { immediate: true })

const periodInvalid = computed(() => !!form.value.workshopStart && !!form.value.workshopEnd && form.value.workshopEnd < form.value.workshopStart)
const valid = computed(() => !!form.value.type.trim() && !!form.value.registration.trim() && !periodInvalid.value)

const save = async () => {
  if (!valid.value) {
    return
  }
  saving.value = true
  registrationError.value = null
  const body = { ...form.value, workshopStart: form.value.workshopStart || null, workshopEnd: form.value.workshopEnd || null }
  const { data, error } = props.aircraft
    ? await useAPI<Aircraft>(`/maintenance/aircraft/${props.aircraft.id}`, { method: 'PUT', body })
    : await useAPI<Aircraft>('/maintenance/aircraft', { method: 'POST', body })
  saving.value = false

  if (error.value) {
    if (error.value.statusCode === 409) {
      registrationError.value = t('maintenance.aircraft.exists')
    } else {
      toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    }
    return
  }
  open.value = false
  emit('saved', data.value as Aircraft)
}
</script>
