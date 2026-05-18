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

const columns = [
  { accessorKey: 'timestamp', header: t('alarm.timestamp'), cell: ({ row }: { row: { original: Alarm } }) => formatUtcTimestamp(row.original.timestamp) },
  { accessorKey: 'rfidId', header: t('alarm.rfidId') },
  { accessorKey: 'internalTime', header: t('alarm.internalTime') },
  { accessorKey: 'status', header: t('alarm.status') },
  { accessorKey: 'rawData', header: t('alarm.rawData') },
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