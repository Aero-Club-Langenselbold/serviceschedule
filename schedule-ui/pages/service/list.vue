<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-8/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('service.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <UTable v-model:expanded="expanded" :data="listData" :columns="columns" :ui="{ tr: 'data-[expanded=true]:bg-(--ui-bg-elevated)/50' }" class="flex-1">
        <template #expanded="{ row }">
          <table class="min-w-full border border-gray-200 mt-4">
            <thead>
              <tr>
                <th class="px-4 py-2 border">{{ $t('service.rangeFrom') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.rangeTo') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.mandatoryAssignees') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.saturdays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.sundays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.publicHolidays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.startTime') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.endTime') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(setting, index) in row.original.settings" :key="index">
                <td class="border px-4 py-2">{{ getLabelForValue(setting.rangeFrom) }}</td>
                <td class="border px-4 py-2">{{ getLabelForValue(setting.rangeTo) }}</td>
                <td class="border px-4 py-2">{{ setting.mandatoryAssignees }}</td>
                <td class="border px-4 py-2 text-center">
                  <UBadge :color="setting.saturdays ? 'success' : 'error'">{{ setting.saturdays ? $t('general.yes') : $t('general.no') }}</UBadge>
                </td>
                <td class="border px-4 py-2 text-center">
                  <UBadge :color="setting.sundays ? 'success' : 'error'">{{ setting.sundays ?  $t('general.yes') : $t('general.no') }}</UBadge>
                </td>
                <td class="border px-4 py-2 text-center">
                  <UBadge :color="setting.publicHolidays ? 'success' : 'error'">{{ setting.publicHolidays ?  $t('general.yes') : $t('general.no') }}</UBadge>
                </td>
                <td class="border px-4 py-2">{{ setting.startTime }}</td>
                <td class="border px-4 py-2">{{ setting.endTime }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </UTable>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'

const { t } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')

const dateRangeItems = ref([
  {
    label: t('general.january'),
    value: '01-01'
  }, {
    label: t('general.february'),
    value: '02-01'
  }, {
    label: t('general.march'),
    value: '03-01'
  }, {
    label: t('general.april'),
    value: '04-01'
  }, {
    label: t('general.may'),
    value: '05-01'
  }, {
    label: t('general.june'),
    value: '06-01'
  }, {
    label: t('general.july'),
    value: '07-01'
  }, {
    label: t('general.august'),
    value: '08-01'
  }, {
    label: t('general.september'),
    value: '09-01'
  }, {
    label: t('general.october'),
    value: '10-01'
  }, {
    label: t('general.november'),
    value: '11-01'
  }, {
    label: t('general.december'),
    value: '12-01'
  }
])

const getLabelForValue = (value: string) => {
  const item = dateRangeItems.value.find(item => item.value === value)
  return item ? item.label : value
}

const listData = ref([])

type Service = {
  id: string
  servicename: string
  settings: [{
    rangeFrom: string
    rangeTo: string
    mandatoryAssignees: number
    saturdays: boolean
    sundays: boolean
    publicHolidays: boolean
    startTime: string
    endTime: string
  }]
}

const columns = computed<TableColumn<Service>[]>(() => [
  {
    id: 'expand',
    cell: ({ row }) =>
      h(UButton, {
        color: 'neutral',
        variant: 'ghost',
        icon: 'i-lucide-chevron-down',
        square: true,
        ui: {
          leadingIcon: [
            'transition-transform',
            row.getIsExpanded() ? 'duration-200 rotate-180' : ''
          ]
        },
        onClick: () => row.toggleExpanded()
      })
  }, {
    accessorKey: 'servicename',
    header: t('service.name')
  }
])

const expanded = ref({ 1: true })

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data, error } = await useAPI(`/service/list`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      listData.value = data.value
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

watchEffect(() => {
  fetchData()
})


definePageMeta({
    middleware: ['authenticated']
})
</script>