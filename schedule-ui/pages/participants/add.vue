<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-md border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('participants.add') }}</h2>
      <form @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('participants.firstname') }}
          </label>
          <UInput v-model="participant.firstname" type="text" :placeholder="$t('participants.firstname')" class="w-full" />
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('participants.lastname') }}
          </label>
          <UInput v-model="participant.lastname" type="text" :placeholder="$t('participants.lastname')" class="w-full" />
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('participants.email') }}
          </label>
          <UInput v-model="participant.email" type="email" :placeholder="$t('participants.email')" class="w-full" />
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('participants.dateOfBirth') }}
          </label>
          <UPopover>
            <UButton color="neutral" variant="subtle" icon="i-lucide-calendar" class="w-full">
              {{ dateOfBirth ? df.format(dateOfBirth.toDate(getLocalTimeZone())) : $t('participants.dateOfBirth') }}
            </UButton>

            <template #content>
              <UCalendar v-model="dateOfBirth" class="p-2" />
            </template>
          </UPopover>
        </div>
        <div class="mb-6 flex items-center ">
          <label class="text-sm font-bold mr-6">
            {{ $t('participants.status') }}
          </label>
          <UCheckbox v-model="status" />
          <UTooltip :delay-duration="0" :text="t('participants.statusInfo')" class="ml-5">
            <UButton icon="i-lucide-message-circle-question" color="secondary" variant="ghost" />
          </UTooltip>
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('participants.services') }}
          </label>
          <USelect multiple v-model="serviceValue" value-key="id" :items="serviceItems" label-key="servicename" class="w-full" />
        </div>
        <div class="flex items-center justify-between">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('participants.save')" type="submit" />
        </div>
      </form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { DateValue } from '@internationalized/date'
import { CalendarDate, DateFormatter, getLocalTimeZone } from '@internationalized/date'
import type { Participant } from '~/types/participant'
import type { Service } from '~/types/service'
const { t } = useI18n()

const df = new DateFormatter(t('app.dateLocale'), {
  dateStyle: 'short'
})

/*const participant = reactive({
  firstname: ref<string | number>(''),
  lastname: ref<string | number>(''),
  email: ref<string | number>(''),
  dateOfBirth: shallowRef<CalendarDate | null>(null),
  status: ref<string | boolean>(true),
  services: ref<string | number>('')
})*/
const participant = ref<Participant> ({
  id: '',
  firstname: '',
  lastname: '',
  email: '',
  dateOfBirth: new Date(),
  status: 'ACTIVE',
  registrationKey: '',
  services: []
})

const dateOfBirth = shallowRef<CalendarDate | DateValue | undefined>(undefined)
const status = ref<boolean>(true)

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const serviceItems = ref<Service[]>([])
const serviceValue = ref<string[]>([])

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null

  participant.value.dateOfBirth = dateOfBirth.value ? dateOfBirth.value.toDate(getLocalTimeZone()) : undefined
  participant.value.status = status.value ? 'ACTIVE' : 'INACTIVE'
  participant.value.services = serviceItems.value.filter((service: { id: string }) => serviceValue.value.includes(service.id))

  await useAPI('/participant/save', { method: 'POST', body: JSON.stringify(participant.value) })
  .then(async (response) => {
    if (response.status.value === "error" && response.error?.value?.statusCode === 409) {
      errorMessage.value = t('participants.exists')
    } else if (response.status.value === "error") {
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

try {
  const { data, error } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = error.value.message
  } else {
    participant.value.services = data.value ?? []
    serviceItems.value = data.value ?? []
  }
} catch (err) {
  errorMessage.value = 'An error occurred while fetching the data.'
}

definePageMeta({
  middleware: ['authenticated', 'admin']
})
</script>
