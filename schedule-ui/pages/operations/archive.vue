<template>
  <AppPage :title="$t('operations.archive.title')" :description="$t('operations.archive.description')" :error-message="errorMessage" width="full" back="/operations" :back-label="$t('operations.overview.title')">
    <div class="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
      <UInput
        v-model="filters.search" icon="i-lucide-search" class="w-full lg:col-span-2" :placeholder="$t('operations.archive.search')"
        :aria-label="$t('operations.archive.search')" data-filter-search @update:model-value="reloadDebounced"
      />
      <USelect v-model="filters.aircraft" :items="aircraftItems" class="w-full" :aria-label="$t('operations.archive.aircraft')" data-filter-aircraft @update:model-value="reload" />
      <USelect v-model="filters.kind" :items="kindFilterItems" class="w-full" :aria-label="$t('operations.archive.kind')" data-filter-kind @update:model-value="reload" />
      <USelect v-model="filters.status" :items="statusFilterItems" class="w-full" :aria-label="$t('operations.archive.status')" data-filter-status @update:model-value="reload" />
      <div class="flex gap-2">
        <UInput v-model="filters.from" type="date" class="w-full" :aria-label="$t('operations.archive.from')" data-filter-from @update:model-value="reload" />
        <UInput v-model="filters.to" type="date" class="w-full" :aria-label="$t('operations.archive.to')" data-filter-to @update:model-value="reload" />
      </div>
    </div>

    <DataTable :data="listData?.content" :columns="columns" />
    <div class="mt-4 flex justify-center">
      <UPagination v-model:page="page" :sibling-count="1" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData" />
    </div>
  </AppPage>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Aircraft } from '~/types/maintenance'
import type { ChecklistSummary } from '~/types/operations'
import type { Page } from '~/types/page'

/** Every checklist ever filled in, searchable; for operations administrators. */
const ALL = 'all'

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const { t } = useI18n()
const { kindLabel, statusLabel, statusColor, kindItems, statusItems } = useOperations()
const { formatDay } = useMaintenance()

const page = ref(1)
const pageSize = ref(20)
const listData = ref<Page<ChecklistSummary> | null>(null)
const aircraft = ref<Aircraft[]>([])
const errorMessage = ref<string | null>(null)
const filters = reactive({ search: '', aircraft: ALL, kind: ALL, status: ALL, from: '', to: '' })

const aircraftItems = computed(() => [{ label: t('operations.archive.allAircraft'), value: ALL },
  ...aircraft.value.map(item => ({ label: `${item.registration} · ${item.type}`, value: item.id }))])
const kindFilterItems = computed(() => [{ label: t('operations.archive.allKinds'), value: ALL }, ...kindItems.value])
const statusFilterItems = computed(() => [{ label: t('operations.archive.allStatuses'), value: ALL }, ...statusItems.value])

const columns = computed<TableColumn<ChecklistSummary>[]>(() => [
  {
    accessorKey: 'date',
    header: t('operations.archive.date'),
    cell: ({ row }) => formatDay(row.original.date),
    meta: { cardTitle: true, class: { td: 'font-medium text-highlighted' } }
  }, {
    accessorKey: 'registration',
    header: t('operations.archive.aircraft'),
    cell: ({ row }) => `${row.original.registration} · ${row.original.aircraftType}`
  }, {
    accessorKey: 'kind',
    header: t('operations.archive.kind'),
    cell: ({ row }) => kindLabel(row.original.kind)
  }, {
    accessorKey: 'status',
    header: t('operations.archive.status'),
    cell: ({ row }) => h(UBadge, { color: statusColor(row.original.status), variant: 'subtle' }, () => statusLabel(row.original.status))
  }, {
    accessorKey: 'createdBy',
    header: t('operations.checklist.createdBy'),
    cell: ({ row }) => row.original.createdBy?.name ?? t('operations.checklist.unknownUser')
  }, {
    id: 'progress',
    header: t('operations.archive.result'),
    cell: ({ row }) => h('span', { class: row.original.notOkCount ? 'text-error font-medium' : '' },
      `${t('operations.checklist.progress', { checked: row.original.checkedCount, total: row.original.itemCount })}`
      + (row.original.notOkCount ? ` · ${t('operations.checklist.notOkCount', row.original.notOkCount)}` : ''))
  }, {
    id: 'actions',
    cell: ({ row }) => h('div', { class: 'flex justify-end' }, [
      h(UButton, { to: `/operations/checklists/${row.original.id}`, color: 'neutral', variant: 'outline', icon: 'i-lucide-eye', 'data-open-checklist': row.original.id },
        () => t('operations.archive.open'))
    ])
  }
])

const query = () => {
  const params = new URLSearchParams({ page: String(page.value - 1), size: String(pageSize.value) })
  if (filters.search.trim()) params.set('search', filters.search.trim())
  if (filters.aircraft !== ALL) params.set('aircraftId', filters.aircraft)
  if (filters.kind !== ALL) params.set('kind', filters.kind)
  if (filters.status !== ALL) params.set('status', filters.status)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  return params.toString()
}

const fetchData = async () => {
  const { data, error } = await useAPI<Page<ChecklistSummary>>(`/operations/checklists?${query()}`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = t('operations.loadError')
    return
  }
  errorMessage.value = null
  listData.value = data.value as Page<ChecklistSummary>
}

const reload = () => {
  page.value = 1
  fetchData()
}

let debounceTimeout: ReturnType<typeof setTimeout>
const reloadDebounced = () => {
  clearTimeout(debounceTimeout)
  debounceTimeout = setTimeout(reload, 400)
}

onMounted(async () => {
  fetchData()
  const { data } = await useAPI<Aircraft[]>('/maintenance/aircraft', { method: 'GET' })
  aircraft.value = data.value ?? []
})

definePageMeta({
  middleware: ['authenticated', 'operations-admin']
})
</script>
