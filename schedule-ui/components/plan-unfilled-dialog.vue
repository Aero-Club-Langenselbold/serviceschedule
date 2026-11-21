<template>
  <UModal v-model:open="open" :title="$t('plan.unfilled.title')" :description="$t('plan.unfilled.description', { name: planName })" :ui="{ content: 'sm:max-w-xl' }">
    <template #body>
      <p class="mb-4 flex items-start gap-2 rounded-lg bg-warning/10 p-3 text-sm text-default">
        <UIcon name="i-lucide-triangle-alert" class="mt-0.5 size-4 shrink-0 text-warning" />
        {{ $t('plan.unfilled.summary', { count: items.length }, items.length) }}
      </p>
      <ol class="space-y-4">
        <li v-for="day in days" :key="day.date">
          <h3 class="mb-1.5 text-sm font-semibold text-highlighted">{{ formatDay(day.date) }}</h3>
          <ul class="divide-y divide-default overflow-hidden rounded-lg border border-default">
            <li v-for="item in day.items" :key="item.serviceId" class="flex items-center justify-between gap-3 px-3 py-2 text-sm">
              <span class="min-w-0">
                <span class="block truncate font-medium text-default">{{ item.servicename }}</span>
                <span class="text-xs text-muted">{{ item.startTime.slice(0, 5) }} – {{ item.endTime.slice(0, 5) }}</span>
              </span>
              <UBadge :color="item.assigned ? 'warning' : 'error'" variant="subtle" class="shrink-0">
                {{ item.assigned ? $t('plan.unfilled.partly', { assigned: item.assigned, required: item.required }) : $t('plan.unfilled.nobody') }}
              </UBadge>
            </li>
          </ul>
        </li>
      </ol>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.close')" @click="open = false" />
        <UButton icon="i-lucide-book-open" :label="$t('plan.unfilled.edit')" :to="`/plan/generated/${planId}`" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { UnfilledServiceDate } from '~/types/plan'

const props = defineProps<{ planId: string, planName: string, items: UnfilledServiceDate[] }>()
const open = defineModel<boolean>('open', { default: false })

const { locale } = useI18n()

// grouped by day, the backend already sorts by day and service
const days = computed(() => {
  const result: { date: string, items: UnfilledServiceDate[] }[] = []
  for (const item of props.items) {
    const last = result[result.length - 1]
    if (last?.date === item.date) {
      last.items.push(item)
    } else {
      result.push({ date: item.date, items: [item] })
    }
  }
  return result
})

const formatDay = (iso: string) => {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(Date.UTC(year!, month! - 1, day!)).toLocaleDateString(locale.value, { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' })
}
</script>
