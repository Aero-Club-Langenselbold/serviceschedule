<template>
  <AppPage
    :title="$t('winch.dayTitle', { date: formatDay(date) })" :error-message="errorMessage" width="lg"
    back="/maintenance/winch/history" :back-label="$t('winch.history.title')"
  >
    <template v-if="day?.today" #actions>
      <UButton to="/maintenance/winch" icon="i-lucide-pencil" :label="$t('winch.openToday')" />
    </template>

    <MaintenanceWinchDay v-if="day" :detail="day" @update="day = $event" @reload="load" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { WinchDay } from '~/types/winch'

/** A page of the winch book, read only; maintenance administrators correct remarks and delete entries. */
const route = useRoute()
const { t } = useI18n()
const { formatDay } = useMaintenance()

const date = computed(() => String(route.params.date ?? ''))
const day = ref<WinchDay | null>(null)
const errorMessage = ref<string | null>(null)

const load = async () => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date.value)) {
    errorMessage.value = t('winch.notFound')
    return
  }
  const { data, error } = await useAPI<WinchDay>(`/maintenance/winch/days/${date.value}`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = error.value.statusCode === 404 ? t('winch.notFound') : t('winch.loadError')
    day.value = null
    return
  }
  errorMessage.value = null
  day.value = data.value as WinchDay
}

watch(date, load)
onMounted(load)

definePageMeta({
  middleware: ['authenticated']
})
</script>
