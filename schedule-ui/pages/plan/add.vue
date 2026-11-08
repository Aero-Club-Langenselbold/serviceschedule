<template>
  <AppPage :title="$t('plan.add')" back="/plan/list" :back-label="$t('plan.backToList')" width="sm">
    <UCard :ui="{ root: 'rounded-xl' }">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <UFormField :label="$t('plan.name')">
          <UInput v-model="plan.name" type="text" class="w-full" />
        </UFormField>
        <UFormField :label="$t('plan.description')">
          <UInput v-model="plan.description" type="text" class="w-full" />
        </UFormField>
        <div class="grid gap-5 sm:grid-cols-2">
          <UFormField :label="$t('plan.dateFrom')">
            <UPopover>
              <UButton color="neutral" variant="outline" icon="i-lucide-calendar" class="w-full justify-start">
                {{ dateFrom ? df.format(dateFrom.toDate(getLocalTimeZone())) : $t('plan.dateFrom') }}
              </UButton>
              <template #content>
                <UCalendar v-model="dateFrom" class="p-2" />
              </template>
            </UPopover>
          </UFormField>
          <UFormField :label="$t('plan.dateUntil')">
            <UPopover>
              <UButton color="neutral" variant="outline" icon="i-lucide-calendar" class="w-full justify-start">
                {{ dateUntil ? df.format(dateUntil.toDate(getLocalTimeZone())) : $t('plan.dateUntil') }}
              </UButton>
              <template #content>
                <UCalendar v-model="dateUntil" class="p-2" />
              </template>
            </UPopover>
          </UFormField>
        </div>
        <UButton block size="lg" :label="$t('plan.save')" type="submit" />
      </form>
    </UCard>
  </AppPage>
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
      errorMessage.value = t('plan.saveError')
    } else {
      successMessage.value = t('plan.saveSuccess')
      errorMessage.value = null

      plan.value.name = ''
      plan.value.description = ''
      plan.value.dateFrom = null
      plan.value.dateUntil = null

      await navigateTo('/plan/details/' + pd.value?.id)
    }
  }
  catch(error) {
    errorMessage.value = t('plan.saveError')
  }
}

definePageMeta({
  middleware: ['authenticated', 'schedule-admin']
})
</script>
