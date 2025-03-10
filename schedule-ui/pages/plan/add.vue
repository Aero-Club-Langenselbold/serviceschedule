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
              {{ plan.dateFrom ? df.format(plan.dateFrom.toDate(getLocalTimeZone())) : $t('plan.dateFrom') }}
            </UButton>

            <template #content>
              <UCalendar v-model="plan.dateFrom" class="p-2" />
            </template>
          </UPopover>
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('plan.dateUntil') }}
          </label>
          <UPopover>
            <UButton color="neutral" variant="subtle" icon="i-lucide-calendar" class="w-full">
              {{ plan.dateUntil ? df.format(plan.dateUntil.toDate(getLocalTimeZone())) : $t('plan.dateUntil') }}
            </UButton>

            <template #content>
              <UCalendar v-model="plan.dateUntil" class="p-2" />
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
import { CalendarDate, DateFormatter, getLocalTimeZone } from '@internationalized/date'
const { t } = useI18n()

const df = new DateFormatter(t('app.dateLocale'), {
  dateStyle: 'short'
})

const plan = reactive({
  name: ref<string | number>(''),
  description: ref<string | number>(''),
  dateFrom: shallowRef<CalendarDate | null>(null),
  dateUntil: shallowRef<CalendarDate | null>(null)
})

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null

  const planData = {
    ...plan,
    dateFrom: plan.dateFrom ? plan.dateFrom.toDate(getLocalTimeZone()).toISOString() : null,
    dateUntil: plan.dateUntil ? plan.dateUntil.toDate(getLocalTimeZone()).toISOString() : null
  }

  await useAPI('/plan/save', { method: 'POST', body: JSON.stringify(planData) })
  .then(async (response) => {
    if (response.status.value === "error" && response.error?.value?.statusCode === 409) {
      errorMessage.value = t('plan.exists')
    } else if (response.status.value === "error") {
      errorMessage.value = t('plan.error')
    } else {
      successMessage.value = t('plan.success')
      errorMessage.value = null

      plan.name = ''
      plan.description = ''
      plan.dateFrom = null
      plan.dateUntil = null
    }
  })
  .catch((error) => {
    console.log(error)
  })
}

definePageMeta({
    middleware: ['authenticated']
})
</script>
