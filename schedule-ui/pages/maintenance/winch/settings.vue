<template>
  <AppPage
    :title="$t('winch.settings.title')" :description="$t('winch.settings.description')" :error-message="errorMessage" width="lg"
    back="/maintenance/winch" :back-label="$t('winch.title')"
  >
    <div v-if="settings" class="grid gap-6 lg:grid-cols-2">
      <MaintenanceWinchOptionList
        kind="ropes" :options="settings.ropes" :title="$t('winch.settings.ropes')" :hint="$t('winch.settings.ropesHint')"
        :placeholder="$t('winch.settings.newRope')" @update="settings = $event"
      />
      <MaintenanceWinchOptionList
        kind="incident-types" :options="settings.incidentTypes" :title="$t('winch.settings.incidentTypes')"
        :hint="$t('winch.settings.incidentTypesHint')" :placeholder="$t('winch.settings.newIncidentType')" @update="settings = $event"
      />
    </div>
  </AppPage>
</template>

<script lang="ts" setup>
import type { WinchSettings } from '~/types/winch'

/** Ropes of the winch and kinds of incidents of the winch book; for maintenance administrators. */
const { t } = useI18n()

const settings = ref<WinchSettings | null>(null)
const errorMessage = ref<string | null>(null)

onMounted(async () => {
  const { data, error } = await useAPI<WinchSettings>('/maintenance/winch/settings', { method: 'GET' })
  if (error.value) {
    errorMessage.value = t('winch.loadError')
    return
  }
  settings.value = data.value as WinchSettings
})

definePageMeta({
  middleware: ['authenticated', 'maintenance-admin']
})
</script>
