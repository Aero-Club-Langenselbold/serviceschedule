<template>
  <AppPage :title="$t('myServices.title')" :description="$t('myServices.description')" :error-message="errorMessage" width="md">
    <template #actions>
      <UButton
        icon="i-lucide-calendar-arrow-down" :label="$t('myServices.download')" :loading="downloading"
        :disabled="!services.length" @click="downloadIcs"
      />
    </template>

    <USwitch v-model="showPast" class="mb-4" :label="$t('myServices.showPast')" :description="$t('myServices.showPastHint')" />

    <div v-if="loaded && !services.length" class="rounded-xl border border-dashed border-default bg-default p-8 text-center">
      <UIcon name="i-lucide-calendar-x" class="mx-auto mb-2 size-8 text-dimmed" />
      <p class="font-medium text-highlighted">{{ showPast ? $t('myServices.emptyAll') : $t('myServices.empty') }}</p>
      <p class="mt-1 text-sm text-muted">{{ $t('myServices.emptyHint') }}</p>
    </div>

    <ul class="space-y-3">
      <li v-for="service in services" :key="service.id" :data-service="service.id">
        <article class="flex gap-4 rounded-xl border border-default bg-default p-4" :class="{ 'opacity-60': isPast(service) }">
          <div class="flex w-14 shrink-0 flex-col items-center justify-center rounded-lg py-1.5" :class="isPast(service) ? 'bg-elevated text-muted' : 'bg-primary/10 text-primary'">
            <span class="text-xs font-medium uppercase">{{ format(service.startTime, { weekday: 'short' }) }}</span>
            <span class="text-xl leading-tight font-semibold">{{ format(service.startTime, { day: 'numeric' }) }}</span>
            <span class="text-xs">{{ format(service.startTime, { month: 'short' }) }}</span>
          </div>
          <div class="min-w-0 flex-1 space-y-1">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="font-semibold text-highlighted">{{ service.serviceName }}</h2>
              <UBadge v-if="isPast(service)" color="neutral" variant="subtle" size="sm">{{ $t('myServices.past') }}</UBadge>
            </div>
            <p class="flex items-center gap-1.5 text-sm text-default">
              <UIcon name="i-lucide-clock" class="size-4 shrink-0 text-muted" />
              {{ format(service.startTime, { day: '2-digit', month: '2-digit', year: 'numeric' }) }},
              {{ format(service.startTime, { hour: '2-digit', minute: '2-digit' }) }} – {{ format(service.endTime, { hour: '2-digit', minute: '2-digit' }) }}
            </p>
            <p class="flex items-center gap-1.5 text-sm text-muted">
              <UIcon name="i-lucide-calendar-days" class="size-4 shrink-0" />
              {{ service.planName }}
            </p>
            <p v-if="service.colleagues.length" class="flex items-start gap-1.5 text-sm text-muted">
              <UIcon name="i-lucide-users" class="mt-0.5 size-4 shrink-0" />
              <span>{{ $t('myServices.with', { names: service.colleagues.join(', ') }) }}</span>
            </p>
          </div>
        </article>
      </li>
    </ul>
  </AppPage>
</template>

<script lang="ts" setup>
import type { PersonalService } from '~/types/plan'

const { t, locale } = useI18n()

definePageMeta({
  middleware: ['authenticated']
})

const services = ref<PersonalService[]>([])
const loaded = ref(false)
const errorMessage = ref<string | null>(null)
const downloading = ref(false)
const showPast = ref(false)

// Times are the club's local times without zone ("2026-05-03T09:00:00") and are shown as they are.
const format = (time: string, options: Intl.DateTimeFormatOptions) => new Date(time).toLocaleString(locale.value, options)
const isPast = (service: PersonalService) => new Date(service.endTime).getTime() < Date.now()

const fetchServices = async () => {
  const { data, error } = await useAPI<PersonalService[]>(`/plan/mine?past=${showPast.value}`, { method: 'GET' })
  errorMessage.value = error.value ? t('myServices.loadError') : null
  services.value = data.value ?? []
  loaded.value = true
}

const downloadIcs = async () => {
  downloading.value = true
  const { data, error } = await useAPI<string | Blob>(`/plan/mine/ics?past=${showPast.value}`, { method: 'GET', responseType: 'blob' })
  downloading.value = false
  if (error.value || !data.value) {
    errorMessage.value = t('myServices.downloadError')
    return
  }
  const blob = data.value instanceof Blob ? data.value : new Blob([data.value], { type: 'text/calendar' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = t('myServices.fileName')
  link.click()
  window.URL.revokeObjectURL(url)
}

watch(showPast, fetchServices)
onMounted(fetchServices)
</script>
