<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-9/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('alarm.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <UTable :data="listData?.content" :columns="columns" class="flex-1" />
      <UPagination v-model:page="page" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData"/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { Alarm } from '~/types/alarm'
import type { Page } from '~/types/page'

const { t } = useI18n()
const { formatUtcTimestamp } = useFormatTimestamp()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const page = ref(1)
const pageSize = ref(20)
const listData = ref<Page<Alarm[]> | null>(null)

const getAlarmMessage = (alarm: Alarm): string => {
  if (alarm.rfidId) {
    return t('alarm.rfidMessage')
  }
  return t('alarm.statusChangeMessage')
}

const getAlarmDetailMessage = (alarm: Alarm): string => {
  if (alarm.rfidId) {
    return t('alarm.rfidDetail') + alarm.rfidId
  }
  const activeFlags: string[] = []
  if (alarm.armed) activeFlags.push(t('alarm.armed'))
  if (alarm.disarmed) activeFlags.push(t('alarm.disarmed'))
  if (alarm.sabotage) activeFlags.push(t('alarm.sabotage'))
  if (alarm.fault) activeFlags.push(t('alarm.fault'))
  if (alarm.alarm) activeFlags.push(t('alarm.alarm'))
  if (alarm.zone1) activeFlags.push(t('alarm.zone1'))
  if (alarm.zone2) activeFlags.push(t('alarm.zone2'))
  if (alarm.zone3) activeFlags.push(t('alarm.zone3'))
  if (alarm.zone4) activeFlags.push(t('alarm.zone4'))
  if (alarm.zone5) activeFlags.push(t('alarm.zone5'))
  if (alarm.zone6) activeFlags.push(t('alarm.zone6'))
  if (alarm.zone7) activeFlags.push(t('alarm.zone7'))
  if (alarm.zone8) activeFlags.push(t('alarm.zone8'))
  return activeFlags.join(', ')
}

const columns = [
  { accessorKey: 'timestamp', header: t('alarm.timestamp'), cell: ({ row }: { row: { original: Alarm } }) => formatUtcTimestamp(row.original.timestamp) },
  { accessorKey: 'message', header: t('alarm.message'), cell: ({ row }: { row: { original: Alarm } }) => getAlarmMessage(row.original) },
  { accessorKey: 'detailMessage', header: t('alarm.detailMessage'), cell: ({ row }: { row: { original: Alarm } }) => getAlarmDetailMessage(row.original) },
]

let sort = 'timestamp'
let direction = 'desc'

const fetchData = async () => {
  try {
    const { data: alarmData, error: alarmError } = await useAPI<Page<Alarm>>(`/alarm/page?page=${page.value - 1}&size=${pageSize.value}&sort=${sort},${direction}`, { method: 'GET' })
    if (alarmError.value && alarmError.value?.statusCode !== 200) {
      errorMessage.value = alarmError.value.data.message
    } else {
      listData.value = alarmData.value
    }
  } catch (err) {
    console.error('Error fetching alarm data:', err)
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