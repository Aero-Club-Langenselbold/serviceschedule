<template>
  <AppPage :title="$t('winch.title')" :description="$t('winch.description')" :error-message="errorMessage" width="lg">
    <template #actions>
      <UButton to="/maintenance/winch/history" color="neutral" variant="outline" icon="i-lucide-history" :label="$t('winch.history.open')" data-history-link />
      <UButton
        v-if="canManageMaintenance && !isKiosk" to="/maintenance/winch/settings" color="neutral" variant="outline" icon="i-lucide-settings"
        :label="$t('winch.settings.title')" data-settings-link
      />
    </template>

    <MaintenanceWinchDay v-if="day" :detail="day" @update="day = $event" @reload="load" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { WinchDay } from '~/types/winch'

/** The page of today in the winch book: every member (also on the kiosk tablet) records incidents, photos and remarks. */
const { t } = useI18n()
const { canManageMaintenance, isKiosk } = storeToRefs(useUserStore())

const day = ref<WinchDay | null>(null)
const errorMessage = ref<string | null>(null)

const load = async () => {
  const { data, error } = await useAPI<WinchDay>('/maintenance/winch/days/today', { method: 'GET' })
  if (error.value) {
    errorMessage.value = t('winch.loadError')
    return
  }
  errorMessage.value = null
  day.value = data.value as WinchDay
}

onMounted(load)

definePageMeta({
  middleware: ['authenticated']
})
</script>
