<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md max-w-full border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('availabilities.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />

      <div class="flex justify-start mb-4 mt-4">
        <UButton color="neutral" variant="subtle" icon="i-lucide-arrow-left" @click="navigateTo('/plan/list')">
          {{ $t('plan.backToList') }}
        </UButton>
      </div>

      <!-- Save button -->
      <div class="flex justify-end mb-4 mt-4">
        <UButton variant="solid" color="primary" icon="i-lucide-save" @click="savePreferences" :loading="saving" >
          {{ $t('availabilities.save') }}
        </UButton>
      </div>

      <Spinner v-if="loading" />

      <!-- Render table only when data is loaded -->
      <UTable v-else sticky :data="data" :columns="columns" class="flex-1 max-h-[600px]" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Participant } from '~/types/participant'
import type { ParticipantAvailability } from '~/types/participantAvailability'
import type { Plan } from '~/types/plan'
import type { Service } from '~/types/service'
import type { ServiceDate } from '~/types/serviceDate'

const { t } = useI18n()
const userStore = useUserStore()
const { user } = storeToRefs(userStore)

const UButton = resolveComponent('UButton')
const USelect = resolveComponent('USelect')

const route = useRoute()
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const id = route.params.id
const loading = ref(true)
const saving = ref(false)

const data = ref<{ date: string, currentState: string, selectedService: Service, preferencesSaved: boolean, participant: Participant }[]>([])
const availableServices = ref<Record<string, Service[]>>({})

const columns = computed<TableColumn<any>[]>(() => [
  {
    accessorKey: 'date',
    header: () => h('th', {}, [h('span', '')]),
    cell: ({ row }) => {
      const date = new Date(row.original.date)
      return date.toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: '2-digit', day: '2-digit' }) // Format date with locale
    }
  }, {
    accessorKey: 'participant.id',
    header: t('availabilities.me'),
    cell: ({ row }) => {
      // Toggle state for each cell
      const toggleStates = ['yes', 'no', 'please']

      const currentState = row.original.currentState
      const preferencesSaved = row.original.preferencesSaved

      const toggleState = async () => {
        const currentIndex = toggleStates.indexOf(currentState)
        row.original.currentState = toggleStates[(currentIndex + 1) % toggleStates.length]

        if (row.original.currentState === 'please') {
          const { data: serviceData } = await useAPI<Service[]>(`/plan/availableServices/${id}/${row.original.date}`, { method: 'GET' })

          if (serviceData.value) {
            availableServices.value[row.original.date] = serviceData.value
          }
        }
      }

      return h(UButton, {
        onClick: toggleState,
        variant: preferencesSaved ? 'solid' : 'subtle',
        color: currentState === 'yes' ? 'primary' : currentState === 'no' ? 'error' : 'secondary',
        icon: currentState === 'yes' ? 'i-lucide-thumbs-up' : currentState === 'no' ? 'i-lucide-thumbs-down' : 'i-lucide-plane'
      }, () => t(`availabilities.${currentState}`))
    }
  }, {
    accessorKey: 'selectedService',
    header: t('availabilities.requestedService'),
    cell: ({ row }) =>{
      if (row.original.currentState === 'please') {
        // fetch available services for this participant
        let services = row.original.participant.services
        let selectedService = services[0].id

        services = services.filter((service: Service) =>
          availableServices.value[row.original.date]?.some(availableService => availableService.id === service.id)
        )
        if (services.length === 0) {
          return h('span', {}, t('availabilities.noServiceAvailable'))
        }

        return h(USelect, {
          defaultValue: row.original.selectedService?.id || selectedService,
          items: services,
          labelKey: 'servicename',
          valueKey: 'id',
          class: 'w-40',
          'onUpdate:open': (selectedId: string) => {
            selectedService = selectedId || services[0]?.id
            row.original.selectedService = services.find((service: Service) => service.id === selectedService)
            console.log('selectedService', row.original.selectedService)
          },
        })
      }

      return null
    }
  }
])

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
      // Eliminate duplicate entries and
      const uniqueDates = new Set<string>()
      planData.value?.serviceDates.forEach((serviceDate: any) => {
        uniqueDates.add(serviceDate.date)
      })
      data.value = Array.from(uniqueDates).map(date => {
        const savedAvailability = (availabilityData.value ?? []).length > 0
        let status = 'no'
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

        const row: any = {
          date: date,
          currentState: status,
          preferencesSaved: savedAvailability,
          participant: participantData.value,
          selectedService: savedService
        }
        return row
      })
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
      successMessage.value = 'Preferences saved successfully.'
      fetchData()
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