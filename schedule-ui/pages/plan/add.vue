<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-md border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('plan.add') }}</h2>
      <form @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('plan.name') }}
          </label>
          <UInput v-model="plan.name" type="text" :placeholder="$t('plan.name')" class="w-full" />
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('plan.description') }}
          </label>
          <UInput v-model="plan.description" type="text" :placeholder="$t('plan.description')" class="w-full" />
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('plan.dateFrom') }}
          </label>
          <UPopover>
            <UButton color="neutral" variant="subtle" icon="i-lucide-calendar" class="w-full">
              {{ dateFrom ? df.format(dateFrom.toDate(getLocalTimeZone())) : $t('plan.dateFrom') }}
            </UButton>

            <template #content>
              <UCalendar v-model="dateFrom" class="p-2" />
            </template>
          </UPopover>
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('plan.dateUntil') }}
          </label>
          <UPopover>
            <UButton color="neutral" variant="subtle" icon="i-lucide-calendar" class="w-full">
              {{ dateUntil ? df.format(dateUntil.toDate(getLocalTimeZone())) : $t('plan.dateUntil') }}
            </UButton>

            <template #content>
              <UCalendar v-model="dateUntil" class="p-2" />
            </template>
          </UPopover>
        </div>

        <div class="flex items-center justify-between">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('plan.save')" type="submit" />
        </div>
      </form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { DateValue } from '@internationalized/date'
import { CalendarDate, DateFormatter, getLocalTimeZone } from '@internationalized/date'
import type { Plan } from '~/types/plan'
const { t } = useI18n()

const df = new DateFormatter(t('app.dateLocale'), {
  dateStyle: 'short'
})

const plan = ref<Plan>({
  id: '',
  name: '',
  description: '',
  dateFrom: null,
  dateUntil: null,
  published: false,
  entries: [],
  serviceDates: []
})

const dateFrom = shallowRef<CalendarDate | DateValue | undefined>(undefined)
const dateUntil = shallowRef<CalendarDate | DateValue | undefined>(undefined)

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null

  plan.value.dateFrom = dateFrom.value ? dateFrom.value.toDate(getLocalTimeZone()) : null
  plan.value.dateUntil = dateUntil.value ? dateUntil.value.toDate(getLocalTimeZone()) : null

  try {
    const { data: pd, error: planError } = await useAPI<Plan>('/plan/save', { method: 'POST', body: JSON.stringify(plan.value) })

    if (planError.value) {
      errorMessage.value = t('plan.error')
    } else {
      successMessage.value = t('plan.success')
      errorMessage.value = null

      plan.value.name = ''
      plan.value.description = ''
      plan.value.dateFrom = null
      plan.value.dateUntil = null

      await navigateTo('/plan/details/' + pd.value?.id)
    }
  }
  catch(error) {
    errorMessage.value = t('plan.error')
  }
}

definePageMeta({
  middleware: ['authenticated', 'schedule-admin']
})
</script>
