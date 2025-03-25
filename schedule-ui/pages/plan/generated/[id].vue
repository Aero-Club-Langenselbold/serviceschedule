<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-6 mb-6">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('plan.generatedDetailsInfo') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <Spinner v-if="loading" />

      <UTable v-else :data="data" :columns="columns" class="flex-1 max-h-[600px]" sticky/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Plan } from '~/types/plan'
const route = useRoute()
const { t, locale } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const id = route.params.id
const loading = ref<Boolean>(true)

const data = ref<PlanTableEntry[] | undefined>([])

type PlanTableEntry = {
  date: Date,
  service: {
    id: string,
    servicename: string,
    participants: {
      id: string,
      firstname: string,
      lastname: string
    }[]
  }[]
}

const columns = computed<TableColumn<PlanTableEntry>[]>(() => [
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
    cell: ({ row }: { row: { getValue: (key: string) => any; original: PlanTableEntry } }) => {
      const participants = row.original.service[index].participants
      const formatted = participants.map(participant => `${participant.lastname}, ${participant.firstname}`).join('<br>')
      return h('div', { innerHTML: formatted })
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

    if (planError?.value?.message === 'error') {
      errorMessage.value = 'An error occurred while fetching the data.'
      return
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
          servicename: service?.servicename || ''
        }
      }).sort((a, b) => a.servicename.localeCompare(b.servicename))

      // Initialize data.value with dates and services
      data.value = uniqueDates.map(date => ({
        date: new Date(date),
        service: uniqueServices.map(service => ({
          ...service,
          participants: []
        })) as PlanTableEntry['service']
      }))

      // Populate participants into the corresponding service
      planData.value.entries.forEach(entry => {
        const matchingRow = data.value?.find(row => row.date.getTime() === new Date(entry.date).getTime())
        if (matchingRow) {
          const matchingService = matchingRow.service.find(service => service.id === entry.service.id)
          if (matchingService) {
            matchingService.participants.push({
              id: entry.participant.id,
              firstname: entry.participant.firstname,
              lastname: entry.participant.lastname
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
  middleware: ['authenticated', 'admin']
})
</script>