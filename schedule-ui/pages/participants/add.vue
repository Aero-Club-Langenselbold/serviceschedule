<template>
  <AppPage :title="$t('participants.add')" back="/participants/list" :back-label="$t('nav.participants.list')" width="sm">
    <UCard :ui="{ root: 'rounded-xl' }">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <div class="grid gap-5 sm:grid-cols-2">
          <UFormField :label="$t('participants.firstname')">
            <UInput v-model="participant.firstname" type="text" autocomplete="off" class="w-full" />
          </UFormField>
          <UFormField :label="$t('participants.lastname')">
            <UInput v-model="participant.lastname" type="text" autocomplete="off" class="w-full" />
          </UFormField>
        </div>
        <UFormField :label="$t('participants.email')">
          <UInput v-model="participant.email" type="email" autocomplete="off" class="w-full" />
        </UFormField>
        <UFormField :label="$t('participants.dateOfBirth')">
          <UPopover>
            <UButton color="neutral" variant="outline" icon="i-lucide-calendar" class="w-full justify-start">
              {{ dateOfBirth ? df.format(dateOfBirth.toDate(getLocalTimeZone())) : $t('participants.dateOfBirth') }}
            </UButton>

            <template #content>
              <UCalendar v-model="dateOfBirth" class="p-2" />
            </template>
          </UPopover>
        </UFormField>
        <UCheckbox v-model="status" :label="$t('participants.active')" :description="$t('participants.statusInfo')" />
        <div class="grid gap-5 sm:grid-cols-2">
          <UFormField :label="$t('participants.minAssignments')">
            <UInput v-model="participant.minAssignments" type="number" min="0" class="w-full" />
          </UFormField>
          <UFormField :label="$t('participants.maxAssignments')">
            <UInput v-model="participant.maxAssignments" type="number" min="0" class="w-full" />
          </UFormField>
        </div>
        <fieldset v-if="serviceItems.length">
          <legend class="mb-2 text-sm font-medium text-default">{{ $t('participants.services') }}</legend>
          <ul class="divide-y divide-default rounded-lg border border-default">
            <li v-for="item in serviceItems" :key="item.service.id" class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 p-3">
              <UCheckbox :model-value="serviceValue.includes(item.service.id)" :label="item.service.servicename" @update:model-value="toggleService(item.service.id)" />
              <UCheckbox v-model="item.autoAssign" :label="$t('participants.autoAssign')" :disabled="!serviceValue.includes(item.service.id)" :ui="{ label: 'font-normal text-muted' }" />
            </li>
          </ul>
        </fieldset>
        <UButton block size="lg" :label="$t('participants.save')" type="submit" />
      </form>
    </UCard>
  </AppPage>
</template>

<script lang="ts" setup>
import type { DateValue } from '@internationalized/date'
import { CalendarDate, DateFormatter, getLocalTimeZone } from '@internationalized/date'
import type { Participant } from '~/types/participant'
import type { ParticipantService } from '~/types/participantService'
import type { Service } from '~/types/service'
const { t } = useI18n()

const df = new DateFormatter(t('app.dateLocale'), {
  dateStyle: 'short'
})

const participant = ref<Participant> ({
  id: '',
  firstname: '',
  lastname: '',
  fullname: '',
  email: '',
  dateOfBirth: new Date(),
  status: 'ACTIVE',
  minAssignments: 2,
  maxAssignments: 5,
  registrationKey: '',
  participantServices: [],
  services: undefined
})

const dateOfBirth = shallowRef<CalendarDate | DateValue | undefined>(undefined)
const status = ref<boolean>(true)

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const serviceItems = ref<ParticipantService[]>([])
const serviceValue = ref<string[]>([])

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null

  participant.value.dateOfBirth = dateOfBirth.value ? dateOfBirth.value.toDate(getLocalTimeZone()) : undefined
  participant.value.status = status.value ? 'ACTIVE' : 'INACTIVE'
  participant.value.participantServices = serviceItems.value.filter((item) => serviceValue.value.includes(item.service.id))

  await useAPI('/participant/save', { method: 'POST', body: JSON.stringify(participant.value) })
  .then(async (response) => {
    if (response.error?.value?.statusCode === 409) {
      errorMessage.value = t('participants.exists')
    } else if (response.error?.value) {
      errorMessage.value = t('participants.error')
    } else {
      successMessage.value = t('participants.success')
      errorMessage.value = null

      participant.value.firstname = ''
      participant.value.lastname = ''
      participant.value.email = ''
      participant.value.dateOfBirth = new Date()
      participant.value.status = 'ACTIVE'
    }
  })
  .catch((error) => {
    console.log(error)
  })
}

const toggleService = (id: string) => {
  if (serviceValue.value.includes(id)) {
    serviceValue.value = serviceValue.value.filter(serviceId => serviceId !== id)
  } else {
    serviceValue.value.push(id)
  }
}

try {
  const { data, error } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = error.value.message
  } else {
    serviceItems.value = (data.value ?? []).map(service => ({
      participant: null,
      service: service,
      autoAssign: true
    }))
  }
} catch (err) {
  errorMessage.value = 'An error occurred while fetching the data.'
}

definePageMeta({
  middleware: ['authenticated', 'schedule-admin']
})
</script>
