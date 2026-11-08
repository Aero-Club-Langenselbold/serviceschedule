<template>
  <AppPage
    :title="$t('availabilities.me')"
    :description="$t('availabilities.hint')"
    back="/plan/list"
    :back-label="$t('plan.backToList')"
    :error-message="errorMessage"
    :success-message="successMessage"
    width="md"
  >
    <Spinner v-if="loading" />

    <template v-else>
      <ul class="space-y-3">
        <li v-for="row in data" :key="row.date" class="rounded-xl border border-default bg-default p-4" data-testid="availability">
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 class="font-semibold text-highlighted">{{ formatDate(row.date) }}</h2>
            <div role="group" :aria-label="formatDate(row.date)" class="grid grid-cols-3 gap-1 rounded-lg bg-elevated p-1 sm:flex">
              <UButton
                v-for="option in options"
                :key="option.value"
                :icon="option.icon"
                :color="row.currentState === option.value ? option.color : 'neutral'"
                :variant="row.currentState === option.value ? 'solid' : 'ghost'"
                :aria-pressed="row.currentState === option.value"
                :label="$t(`availabilities.${option.value}`)"
                size="sm"
                class="flex-col gap-1 text-xs sm:flex-row sm:text-sm"
                @click="setState(row, option.value)"
              />
            </div>
          </div>
          <UFormField v-if="row.currentState === 'please'" :label="$t('availabilities.requestedService')" class="mt-3">
            <span v-if="!servicesFor(row).length" class="text-sm text-muted">{{ $t('availabilities.noServiceAvailable') }}</span>
            <USelect
              v-else
              :model-value="row.selectedService?.id"
              :items="servicesFor(row)"
              label-key="servicename"
              value-key="id"
              class="w-full sm:w-64"
              @update:model-value="selectService(row, $event)"
            />
          </UFormField>
        </li>
      </ul>

      <div class="sticky bottom-0 -mx-4 mt-4 flex items-center justify-between gap-3 border-t border-default bg-default/95 px-4 py-3 backdrop-blur sm:static sm:mx-0 sm:rounded-xl sm:border">
        <span class="flex items-center gap-1.5 text-sm" :class="isSaved ? 'text-success' : 'text-muted'">
          <UIcon :name="isSaved ? 'i-lucide-circle-check' : 'i-lucide-circle-dashed'" class="size-4 shrink-0" />
          {{ isSaved ? $t('availabilities.saved') : $t('availabilities.unsaved') }}
        </span>
        <UButton icon="i-lucide-save" :loading="saving" @click="savePreferences">
          {{ $t('availabilities.save') }}
        </UButton>
      </div>
    </template>
  </AppPage>
</template>

<script lang="ts" setup>
import type { Participant } from '~/types/participant'
import type { ParticipantAvailability } from '~/types/participantAvailability'
import type { Plan } from '~/types/plan'
import type { Service } from '~/types/service'
import type { ServiceDate } from '~/types/serviceDate'

type State = 'yes' | 'no' | 'please'
type Row = {
  date: string
  currentState: State
  selectedService: Service | null
  preferencesSaved: boolean
  participant: Participant
}

const { t, locale } = useI18n()
const userStore = useUserStore()
const { user } = storeToRefs(userStore)

const route = useRoute()
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const id = route.params.id
const loading = ref(true)
const saving = ref(false)
const dirty = ref(false)

const data = ref<Row[]>([])
const availableServices = ref<Record<string, Service[]>>({})

const options = [
  { value: 'yes', icon: 'i-lucide-thumbs-up', color: 'primary' },
  { value: 'no', icon: 'i-lucide-thumbs-down', color: 'error' },
  { value: 'please', icon: 'i-lucide-star', color: 'secondary' }
] as const

const isSaved = computed(() => !dirty.value && data.value.some(row => row.preferencesSaved))

const formatDate = (date: string) => new Date(date).toLocaleDateString(locale.value, {
  weekday: 'long', year: 'numeric', month: '2-digit', day: '2-digit'
})

const servicesFor = (row: Row): Service[] => {
  const services = (row.participant.services ?? []) as Service[]
  return services.filter(service => availableServices.value[row.date]?.some(available => available.id === service.id))
}

const setState = async (row: Row, state: State) => {
  row.currentState = state
  dirty.value = true
  if (state === 'please') {
    await fetchAvailableServices(row.date)
    row.selectedService = row.selectedService ?? servicesFor(row)[0] ?? null
  } else {
    row.selectedService = null
  }
}

const selectService = (row: Row, serviceId: string) => {
  row.selectedService = servicesFor(row).find(service => service.id === serviceId) ?? null
  dirty.value = true
}

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  try {
    const { data: participantData, error: participantError } = await useAPI<Participant>('/participant/fetchByMail/' + user.value?.username, { method: 'GET' })
    const { data: planData, error: planError } = await useAPI<Plan>(`/plan/fetch/` + id, { method: 'GET' })
    const { data: availabilityData, error: availabilityError } = await useAPI<ParticipantAvailability[]>(`/plan/participantAvailability/fetch/${id}/${participantData.value?.id}`, { method: 'GET' })
    if (participantError.value || planError.value || availabilityError.value) {
      errorMessage.value = "error while fetching data"
    } else {
      planData.value?.serviceDates.sort((a: ServiceDate, b: ServiceDate) => new Date(a.date).getTime() - new Date(b.date).getTime())
      // Eliminate duplicate entries
      const uniqueDates = new Set<string>()
      planData.value?.serviceDates.forEach((serviceDate: any) => {
        uniqueDates.add(serviceDate.date)
      })
      data.value = Array.from(uniqueDates).map(date => {
        const savedAvailability = (availabilityData.value ?? []).length > 0
        let status: State = 'no'
        let savedService = null

        if (savedAvailability) {
          const savedDate = availabilityData.value?.find((availability: any) => availability.date === date)
          if (savedDate) {
            status = savedDate.requestedService !== null ? 'please' : 'yes'
            savedService = savedDate.requestedService
          } else {
            status = 'no'
          }
        } else {
          status = participantData.value?.status === 'ACTIVE' ? 'yes' : 'no'
        }

        if (status === 'please') {
          fetchAvailableServices(date)
        }

        return {
          date,
          currentState: status,
          preferencesSaved: savedAvailability,
          participant: participantData.value as Participant,
          selectedService: savedService
        }
      })
      dirty.value = false
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  } finally {
    loading.value = false
  }
}

const fetchAvailableServices = async (date: string) => {
  const { data: serviceData } = await useAPI<Service[]>(`/plan/availableServices/${id}/${date}`, { method: 'GET' })
  if (serviceData.value) {
    availableServices.value[date] = serviceData.value || []
  }
}

const savePreferences = async () => {
  errorMessage.value = null
  successMessage.value = null
  saving.value = true

  const payload = data.value
    .filter(row => row.currentState === 'yes' || row.currentState === 'please')
    .map(row => {
      return {
        date: new Date(row.date),
        requestedService: row.selectedService,
        participant: {
          id: row.participant.id
        },
        plan: {
          id: id
        }
      }
    })

  try {
    const { error } = await useAPI('/plan/participantAvailability/save/' + id, { method: 'POST', body: JSON.stringify(payload) })
    if (error.value?.statusCode == 400) {
      errorMessage.value = t(error.value.data.message) + error.value.data.value
    } else if (error.value) {
      errorMessage.value = 'An error occurred while saving the preferences.'
    } else {
      await fetchData()
      successMessage.value = 'Preferences saved successfully.'
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while saving the preferences.'
  } finally {
    saving.value = false
  }
}

watchEffect(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated']
})
</script>