<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-6 mb-6">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('plan.statisticsInfo') }}</h2>
      <UButton color="neutral" variant="subtle" icon="i-lucide-arrow-left" @click="navigateTo('/plan/list')" class="mr-4">
        {{ $t('plan.backToList') }}
      </UButton>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />

      <Spinner v-if="loading" />

      <UTable v-else :data="data" :columns="columns" class="flex-1 max-h-[600px]" sticky />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Column } from '@tanstack/vue-table'
const route = useRoute()

const { t } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const loading = ref(true)

import type { Participant } from '~/types/participant'
import type { Service } from '~/types/service'

const UButton = resolveComponent('UButton')

type PlanStatistics = {
  participant: Participant,
  serviceCount: Map<string, number>,
  totalCount: number
}
const services = ref<Service[]>([])
const data = ref<any[]>([])

const columns = computed<TableColumn<any>[]>(() => {
  const participantColumn : TableColumn<any> = {
    accessorKey: 'participant',
    header: ({ column }) => getHeader(column, t('participants.fullname'))
  }

  const serviceColumns: TableColumn<any>[] = services.value?.map(service => ({
    accessorKey: service.servicename,
    header: ({ column }) => getHeader(column, service.servicename),
    cell: ({ row }: { row: { getValue: (key: string) => any; original: any } }) => {
      const serviceCount = row.original[service.servicename]
      return serviceCount || 0
    }
  }))

  const totalCountColumn: TableColumn<any> = {
    accessorKey: 'totalCount',
    header: ({ column }) => getHeader(column, t('plan.statistics.totalCount'))
  }

  return [participantColumn, ...serviceColumns, totalCountColumn]
})

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  try {
    // fetch the plan details and all services
    const { data: statsData, error: statsError } = await useAPI<PlanStatistics[]>(`/plan/statistics/` + route.params.id, { method: 'GET' })
    const { data: servicesData, error: servicesError } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })

    if (statsError.value || servicesError.value) {
      errorMessage.value = "error while fetching data"
    } else {
      services.value = servicesData.value ? servicesData.value : []

      data.value = statsData.value?.map(stat => {
        const row: Record<string, any> = {
          participant: stat.participant.fullname,
          totalCount: stat.totalCount
        }

        services.value.forEach(service => {
          const serviceCountMap = new Map(Object.entries(stat.serviceCount))
          row[service.servicename] = serviceCountMap.get(service.id) || 0
        })

        return row
      }) || []
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
    console.log(err)
  } finally {
    loading.value = false
  }
}

function getHeader(column: Column<any>, label: string) {
  const isSorted = column.getIsSorted()

  return h(
    UButton,
    {
      color: 'neutral',
      variant: 'ghost',
      label,
      icon: isSorted
        ? isSorted === 'asc'
          ? 'i-lucide-arrow-up-narrow-wide'
          : 'i-lucide-arrow-down-wide-narrow'
        : 'i-lucide-arrow-up-down',
      onclick: () => {
        if (!isSorted || isSorted === 'desc') {
          column.toggleSorting(false)
        } else if (isSorted === 'asc') {
          column.toggleSorting(true)
        }
      },
      class: '-mx-2.5',
      'aria-label': `Sort by ${isSorted === 'asc' ? 'descending' : 'ascending'}`
    }
  )
}

onMounted(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated', 'admin']
})
</script>
