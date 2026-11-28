<template>
  <AppPage
    :title="$t('winch.history.title')" :description="$t('winch.history.description')" :error-message="errorMessage" width="lg"
    back="/maintenance/winch" :back-label="$t('winch.title')"
  >
    <template #actions>
      <USelect v-model="year" :items="yearItems" class="w-32" :aria-label="$t('winch.history.year')" data-year-filter @update:model-value="changeYear" />
    </template>

    <DataTable :data="listData?.content" :columns="columns" />
    <div v-if="(listData?.totalElements ?? 0) > pageSize" class="mt-4 flex justify-center">
      <UPagination v-model:page="page" :sibling-count="1" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="changePage" />
    </div>
  </AppPage>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Page } from '~/types/page'
import type { WinchDaySummary } from '~/types/winch'

/** All earlier pages of the winch book, by year (the current one by default), newest first. Year and page live in the URL. */
const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UIcon = resolveComponent('UIcon')

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { formatDay } = useMaintenance()

const currentYear = new Date().getFullYear()
const pageSize = 20
const year = ref(Number(route.query.year) || currentYear)
const page = ref(Math.max(1, Number(route.query.page) || 1))
const years = ref<number[]>([currentYear])
const listData = ref<Page<WinchDaySummary> | null>(null)
const errorMessage = ref<string | null>(null)

const yearItems = computed(() => [...new Set([...years.value, year.value])].sort((a, b) => b - a).map(value => ({ label: String(value), value })))

const columns = computed<TableColumn<WinchDaySummary>[]>(() => [
  {
    accessorKey: 'date',
    header: t('winch.history.date'),
    cell: ({ row }) => formatDay(row.original.date),
    meta: { cardTitle: true, class: { td: 'font-medium text-highlighted' } }
  }, {
    accessorKey: 'drivers',
    header: t('winch.drivers'),
    cell: ({ row }) => row.original.drivers.map(driver => driver.name).join(', ') || '–'
  }, {
    accessorKey: 'incidents',
    header: t('winch.history.incidents'),
    cell: ({ row }) => row.original.incidents.length
      ? h('div', { class: 'flex flex-wrap gap-1' }, row.original.incidents.map(count =>
          h(UBadge, { color: 'warning', variant: 'subtle', key: count.typeId }, () => `${count.name} × ${count.count}`)))
      : h('span', { class: 'text-muted' }, t('winch.history.noIncidents'))
  }, {
    id: 'extras',
    header: t('winch.history.extras'),
    cell: ({ row }) => h('div', { class: 'flex flex-wrap items-center gap-3 text-sm text-muted' }, [
      row.original.photoCount ? h('span', { class: 'inline-flex items-center gap-1' }, [h(UIcon, { name: 'i-lucide-image', class: 'size-4' }), String(row.original.photoCount)]) : null,
      row.original.hasRemarks ? h('span', { class: 'inline-flex items-center gap-1' }, [h(UIcon, { name: 'i-lucide-message-square-text', class: 'size-4' }), t('winch.history.remarks')]) : null
    ])
  }, {
    id: 'actions',
    cell: ({ row }) => h('div', { class: 'flex justify-end' }, [
      h(UButton, { to: winchDayPath(row.original.date), color: 'neutral', variant: 'outline', icon: 'i-lucide-eye', 'data-open-day': row.original.date },
        () => t('winch.history.openDay'))
    ])
  }
])

const fetchData = async () => {
  const params = new URLSearchParams({ year: String(year.value), page: String(page.value - 1), size: String(pageSize) })
  const { data, error } = await useAPI<Page<WinchDaySummary>>(`/maintenance/winch/days?${params}`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = t('winch.loadError')
    return
  }
  errorMessage.value = null
  listData.value = data.value as Page<WinchDaySummary>
}

const syncQuery = () => router.replace({ query: {
  ...(year.value !== currentYear ? { year: String(year.value) } : {}),
  ...(page.value > 1 ? { page: String(page.value) } : {})
} })

const changeYear = () => {
  page.value = 1
  syncQuery()
  fetchData()
}

const changePage = () => {
  syncQuery()
  fetchData()
}

onMounted(async () => {
  fetchData()
  const { data } = await useAPI<number[]>('/maintenance/winch/years', { method: 'GET' })
  if (Array.isArray(data.value) && data.value.length) {
    years.value = data.value
  }
})

definePageMeta({
  middleware: ['authenticated']
})
</script>
