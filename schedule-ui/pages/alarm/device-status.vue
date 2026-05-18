<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-9/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('alarm.deviceStatus.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <UTable :data="listData?.content" :columns="columns" class="flex-1" />
      <UPagination v-model:page="page" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData"/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { AlarmDeviceStatus } from '~/types/alarm'
import type { Page } from '~/types/page'

const { t } = useI18n()
const { formatUtcTimestamp } = useFormatTimestamp()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const page = ref(1)
const pageSize = ref(20)
const listData = ref<Page<AlarmDeviceStatus[]> | null>(null)

const columns = [
  { accessorKey: 'timestamp', header: t('alarm.deviceStatus.timestamp'), cell: ({ row }: { row: { original: AlarmDeviceStatus } }) => formatUtcTimestamp(row.original.timestamp) },
  { accessorKey: 'betrieb', header: t('alarm.deviceStatus.betrieb') },
  { accessorKey: 'scharf', header: t('alarm.deviceStatus.scharf') },
  { accessorKey: 'unscharf', header: t('alarm.deviceStatus.unscharf') },
  { accessorKey: 'sabotage', header: t('alarm.deviceStatus.sabotage') },
  { accessorKey: 'stoerung', header: t('alarm.deviceStatus.stoerung') },
  { accessorKey: 'alarm', header: t('alarm.deviceStatus.alarm') },
  { accessorKey: 'zone1', header: t('alarm.deviceStatus.zone1') },
  { accessorKey: 'zone2', header: t('alarm.deviceStatus.zone2') },
  { accessorKey: 'zone3', header: t('alarm.deviceStatus.zone3') },
  { accessorKey: 'zone4', header: t('alarm.deviceStatus.zone4') },
  { accessorKey: 'zone5', header: t('alarm.deviceStatus.zone5') },
  { accessorKey: 'zone6', header: t('alarm.deviceStatus.zone6') },
  { accessorKey: 'zone7', header: t('alarm.deviceStatus.zone7') },
  { accessorKey: 'zone8', header: t('alarm.deviceStatus.zone8') },
]

let sort = 'timestamp'
let direction = 'desc'

const fetchData = async () => {
  try {
    const { data: statusData, error: statusError } = await useAPI<Page<AlarmDeviceStatus>>(`/alarm/status/page?page=${page.value - 1}&size=${pageSize.value}&sort=${sort},${direction}`, { method: 'GET' })
    if (statusError.value && statusError.value?.statusCode !== 200) {
      errorMessage.value = statusError.value.data.message
    } else {
      listData.value = statusData.value
    }
  } catch (err) {
    console.error('Error fetching device status data:', err)
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

watchEffect(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated', 'alarm-admin']
})
</script>
