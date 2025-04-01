<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-6 mb-6">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('plan.generatedDetailsInfo') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />

      <div class="flex justify-start mb-4 mt-4">
        <UButton color="neutral" variant="subtle" icon="i-lucide-arrow-left" @click="navigateTo('/plan/list')">
          {{ $t('plan.backToList') }}
        </UButton>
      </div>

      <Spinner v-if="loading" />

      <UTable v-else :data="data" :columns="columns" class="flex-1 max-h-[600px]" sticky/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { ParticipantAvailability } from '~/types/participantAvailability'
import type { Plan } from '~/types/plan'
const route = useRoute()
const { t, locale } = useI18n()

const userStore = useUserStore()
const { user } = storeToRefs(userStore)

const UButton = resolveComponent('UButton')
const UInput = resolveComponent('UInput')
const USelect = resolveComponent('USelect')

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const id = route.params.id
const loading = ref<Boolean>(true)

const data = ref<PlanTableEntryRow[] | undefined>([])
const editingState = reactive<{ [key: string]: boolean }>({})
const availabilityData = ref<ParticipantAvailability[]>([])

type PlanTableEntryRow = {
  date: Date,
  service: {
    id: string,
    entryIds: string[],    // all plan entry ids associated with this date
    servicename: string,
    startTime: Date | undefined,
    endTime: Date | undefined,
    participants: {   // all participants associated with this service and date
      id: string,
      firstname: string,
      lastname: string,
      fullname: string
    }[]
  }[]
}

const columns = computed<TableColumn<PlanTableEntryRow>[]>(() => [
  {
    accessorKey: 'date',
    header: t('plan.date'),
    cell: ({ row }) => {
      return new Date(row.getValue('date')).toLocaleString(locale.value, {
        weekday: 'short',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    }
  },
  ...(data.value?.[0]?.service ?? []).map((service, index) => ({
    accessorKey: `service[${index}].servicename`,
    header: service.servicename,
    cell: ({ row }: { row: { getValue: (key: string) => any; original: PlanTableEntryRow } }) => {
      const service = row.original.service[index]
      const cellIndex = service.entryIds.join('-')
      const participants = service.participants
      const editButton = h(UButton, {
        variant: 'subtle',
        icon: 'i-lucide-edit',
        size: 'sm',
        class: 'mr-2',
        onClick: () => editingState[cellIndex] = true
      })
      const abortButton = h(UButton, {
        color: 'error',
        variant: 'subtle',
        icon: 'i-lucide-x',
        size: 'sm',
        class: 'mr-2',
        onClick: () => editingState[cellIndex] = false
      })
      const saveButton = h(UButton, {
        variant: 'subtle',
        icon: 'i-lucide-save',
        size: 'sm',
        class: 'mr-2',
        onClick: () => saveEntry()
      })
      const startTime = new Date(service.startTime ?? '').toLocaleString(locale.value, {
        hour: '2-digit',
        minute: '2-digit'
      })
      const endTime = new Date(service.endTime ?? '').toLocaleString(locale.value, {
        hour: '2-digit',
        minute: '2-digit'
      })
      const hours = startTime + ' - ' + endTime
      const startTimeInput = h(UInput, {
        modelValue: startTime,
        type: 'time'
      })
      const endTimeInput = h(UInput, {
        modelValue: endTime,
        type: 'time'
      })

      const saveEntry = async () => {
        // TODO: implement save entry logic
        editingState[cellIndex] = false
      }

      const elements: any[] = []
      if (!editingState[cellIndex]) {
        if (user.value?.roles.includes('ROLE_ADMIN')) {
          elements.push(editButton)
          elements.push(h('br'))
        }
        elements.push(h('label', {}, hours))
        elements.push(h('br'))
        participants.forEach(participant => {
          elements.push(h('label', {}, `${participant.lastname}, ${participant.firstname}`))
          elements.push(h('br'))
        })
      } else {
        const allParticipants = Array.from(
          new Map(
            availabilityData.value?.filter(availability => availability.participant.services?.some(s => s.id === service.id))
            .map(availability => [availability.participant.id, availability.participant])
          ).values()
        )
        elements.push(abortButton)
        elements.push(saveButton)
        elements.push(h('br'))
        elements.push(startTimeInput)
        elements.push(" - ")
        elements.push(endTimeInput)

        participants.forEach(participant => {
          elements.push(h('br'))
          elements.push(h(USelect, {
            modelValue: participant.id,
            items: allParticipants,
            labelKey: 'fullname',
            valueKey: 'id',
            class: 'w-full',
            'onUpdate:modelValue': (newValue: string) => {
              participant.id = newValue
            }
          }))
        })
      }
      return elements
    }
  })) || []
])

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  data.value = []

  try {
    // fetch the plan details and all services
    const { data: planData, error: planError } = await useAPI<Plan>(`/plan/fetch/` + id, { method: 'GET' })
    const { data: ad, error: availabilityError } = await useAPI<ParticipantAvailability[]>(`/plan/participantAvailability/fetch/` + id, { method: 'GET' })

    if (planError?.value?.message === 'error' || availabilityError?.value?.message === 'error') {
      errorMessage.value = 'An error occurred while fetching the data.'
      return
    }

    if (ad.value) {
      availabilityData.value = ad.value
    }

    if (planData.value?.entries) {
      // Extract unique dates
      const uniqueDates = Array.from(
        new Set(planData.value.entries.map(entry => new Date(entry.date).toISOString())
        )
      ).sort((a, b) => new Date(a).getTime() - new Date(b).getTime())

      // Extract unique services
      const uniqueServices = Array.from(
        new Set(planData.value.entries.map(entry => entry.service.id))
      ).map(serviceId => {
        const service = planData.value?.entries.find(entry => entry.service.id === serviceId)?.service
        return {
          id: service?.id || '',
          servicename: service?.servicename || '',
          startTime: undefined,
          endTime: undefined,
        }
      }).sort((a, b) => a.servicename.localeCompare(b.servicename))

      // Initialize data.value with dates and services
      data.value = uniqueDates.map(date => ({
        date: new Date(date),
        service: uniqueServices.map(service => ({
          ...service,
          entryIds: [],
          participants: []
        })) as PlanTableEntryRow['service']
      }))

      // Populate participants into the corresponding service
      planData.value.entries.forEach(entry => {
        const matchingRow = data.value?.find(row => row.date.getTime() === new Date(entry.date).getTime())
        if (matchingRow) {
          const matchingService = matchingRow.service.find(service => service.id === entry.service.id)
          if (matchingService) {
            matchingService.entryIds.push(entry.id)
            matchingService.startTime = entry.startTime
            matchingService.endTime = entry.endTime
            matchingService.participants.push({
              id: entry.participant.id,
              firstname: entry.participant.firstname,
              lastname: entry.participant.lastname,
              fullname: entry.participant.lastname + ', ' + entry.participant.firstname
            })
          }
        }
      })
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated']
})
</script>