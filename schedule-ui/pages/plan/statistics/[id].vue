<template>
  <AppPage :title="$t('plan.statisticsInfo')" back="/plan/list" :back-label="$t('plan.backToList')" :error-message="errorMessage" :success-message="successMessage" width="md">
    <Spinner v-if="loading" />
    <DataTable v-else :data="data" :columns="columns" />
  </AppPage>
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
    meta: { label: t('participants.fullname'), cardTitle: true },
    header: ({ column }) => getHeader(column, t('participants.fullname'))
  }

  const serviceColumns: TableColumn<any>[] = services.value?.map(service => ({
    accessorKey: service.servicename,
    meta: { label: service.servicename },
    header: ({ column }) => getHeader(column, service.servicename),
    cell: ({ row }: { row: { getValue: (key: string) => any; original: any } }) => {
      const serviceCount = row.original[service.servicename]
      return serviceCount || 0
    }
  }))

  const totalCountColumn: TableColumn<any> = {
    accessorKey: 'totalCount',
    meta: { label: t('plan.statistics.totalCount') },
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
    }
  )
}

onMounted(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated', 'schedule-admin']
})
</script>
