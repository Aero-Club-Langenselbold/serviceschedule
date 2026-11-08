<template>
  <ul class="space-y-3">
    <li v-for="(setting, index) in settings" :key="index" class="rounded-lg border border-default bg-default p-3">
      <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <UFormField :label="$t('service.rangeFrom')">
          <USelect v-if="editable" v-model="setting.rangeFrom" :items="dateRangeItems" class="w-full" />
          <span v-else class="text-sm text-default">{{ monthLabel(setting.rangeFrom) }}</span>
        </UFormField>
        <UFormField :label="$t('service.rangeTo')">
          <USelect v-if="editable" v-model="setting.rangeTo" :items="dateRangeItems" class="w-full" />
          <span v-else class="text-sm text-default">{{ monthLabel(setting.rangeTo) }}</span>
        </UFormField>
        <UFormField :label="$t('service.startTime')">
          <UInput v-if="editable" v-model="setting.startTime" type="time" class="w-full" />
          <span v-else class="text-sm text-default">{{ setting.startTime }}</span>
        </UFormField>
        <UFormField :label="$t('service.endTime')">
          <UInput v-if="editable" v-model="setting.endTime" type="time" class="w-full" />
          <span v-else class="text-sm text-default">{{ setting.endTime }}</span>
        </UFormField>
        <UFormField :label="$t('service.mandatoryAssignees')">
          <UInput v-if="editable" v-model="setting.mandatoryAssignees" type="number" min="0" class="w-full" />
          <span v-else class="text-sm text-default">{{ setting.mandatoryAssignees }}</span>
        </UFormField>
      </div>
      <div class="mt-3 flex flex-wrap items-center gap-x-5 gap-y-2">
        <template v-for="day in days" :key="day">
          <UCheckbox v-if="editable" v-model="setting[day]" :label="$t(`service.${day}`)" />
          <UBadge v-else :color="setting[day] ? 'success' : 'neutral'" variant="subtle" :icon="setting[day] ? 'i-lucide-check' : 'i-lucide-minus'">
            {{ $t(`service.${day}`) }}
            <span class="sr-only">{{ setting[day] ? $t('general.yes') : $t('general.no') }}</span>
          </UBadge>
        </template>
        <UButton
          v-if="editable"
          color="error"
          variant="ghost"
          icon="i-lucide-trash"
          :label="$t('service.removeSetting')"
          class="ml-auto"
          @click="$emit('remove', index)"
        />
      </div>
    </li>
  </ul>
</template>

<script lang="ts" setup>
import type { Settings } from '~/types/settings'

defineProps<{
  settings: Settings[]
  editable?: boolean
}>()

defineEmits<{
  remove: [index: number]
}>()

const days = ['saturdays', 'sundays', 'publicHolidays'] as const

const dateRangeItems = useDateRangeItems()
const monthLabel = (value: string) => dateRangeItems.value.find(item => item.value === value)?.label ?? value
</script>
