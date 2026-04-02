<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('service.add') }}</h2>
      <form @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('service.name') }}
          </label>
          <UInput v-model="service.servicename" type="text" :placeholder="$t('service.name')" class="w-full" />
        </div>
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('service.addAtImport') }}
          </label>
          <UCheckbox v-model="service.addAtImport" />
        </div>

        <UButton @click.prevent="addSetting" color="neutral" variant="subtle" class="mt-4">
          {{ $t('service.addSetting') }}
        </UButton>

        <div class="overflow-x-auto mt-4">
          <table class="min-w-full ">
            <thead>
              <tr>
                <th class="px-4 py-2 border">{{ $t('service.rangeFrom') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.rangeTo') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.mandatoryAssignees') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.saturdays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.sundays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.publicHolidays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.startTime') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.endTime') }}</th>
                <th class="px-4 py-2 border"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(setting, index) in service.settings" :key="index">
                <td class="border px-4 py-2">
                  <USelect v-model="setting.rangeFrom" :items="dateRangeItems" class="w-48" />
                </td>
                <td class="border px-4 py-2">
                  <USelect v-model="setting.rangeTo" :items="dateRangeItems" class="w-48" />
                </td>
                <td class="border px-4 py-2">
                  <UInput v-model="setting.mandatoryAssignees" type="number" :placeholder="$t('service.mandatoryAssignees')" class="w-full" />
                </td>
                <td class="border px-4 py-2 text-center">
                  <UCheckbox v-model="setting.saturdays" />
                </td>
                <td class="border px-4 py-2 text-center">
                  <UCheckbox v-model="setting.sundays" />
                </td>
                <td class="border px-4 py-2 text-center">
                  <UCheckbox v-model="setting.publicHolidays" />
                </td>
                <td class="border px-4 py-2">
                  <UInput v-model="setting.startTime" type="time" :placeholder="$t('service.startTime')" class="w-full" />
                </td>
                <td class="border px-4 py-2">
                  <UInput v-model="setting.endTime" type="time" :placeholder="$t('service.endTime')" class="w-full" />
                </td>
                <td class="border px-4 py-2 text-center">
                  <UButton @click.prevent="removeSetting(index)" color="error" variant="subtle" leading-icon="i-lucide-trash" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex items-center justify-between mt-6">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('service.save')" type="submit" />
        </div>
      </form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { Service } from '~/types/service'

const { t } = useI18n()
const dateRangeItems = useDateRangeItems()

const service = ref<Service>({
  id: '',
  servicename: '',
  addAtImport: false,
  settings: [
    {
      rangeFrom: '',
      rangeTo: '',
      mandatoryAssignees: 0,
      saturdays: false,
      sundays: false,
      publicHolidays: false,
      startTime: '',
      endTime: ''
    }
  ]
})

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const addSetting = () => {
  service.value.settings.push({
    rangeFrom: '',
    rangeTo: '',
    mandatoryAssignees: 0,
    saturdays: false,
    sundays: false,
    publicHolidays: false,
    startTime: '',
    endTime: ''
  })
}

const removeSetting = (index: number) => {
  service.value.settings.splice(index, 1)
}

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null

  await useAPI('/service/save', { method: 'POST', body: JSON.stringify(service.value) })
  .then(async (response) => {
    if (response.status.value === "error" && response.error?.value?.statusCode === 409) {
      errorMessage.value = t('service.exists')
    } else if (response.status.value === "error") {
      errorMessage.value = t('service.error')
    } else {
      successMessage.value = t('service.success')
      errorMessage.value = null

      service.value.servicename = ''
      service.value.settings = []
    }
  })
  .catch((error) => {
    console.log(error)
  })
}

definePageMeta({
  middleware: ['authenticated', 'schedule-admin']
})
</script>
