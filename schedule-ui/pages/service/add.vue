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
const { t } = useI18n()

const dateRangeItems = ref([
  {
    label: t('general.january'),
    value: '01-01'
  }, {
    label: t('general.february'),
    value: '02-01'
  }, {
    label: t('general.march'),
    value: '03-01'
  }, {
    label: t('general.april'),
    value: '04-01'
  }, {
    label: t('general.may'),
    value: '05-01'
  }, {
    label: t('general.june'),
    value: '06-01'
  }, {
    label: t('general.july'),
    value: '07-01'
  }, {
    label: t('general.august'),
    value: '08-01'
  }, {
    label: t('general.september'),
    value: '09-01'
  }, {
    label: t('general.october'),
    value: '10-01'
  }, {
    label: t('general.november'),
    value: '11-01'
  }, {
    label: t('general.december'),
    value: '12-01'
  }
])

const service = reactive({
  servicename: ref<string | number>(''),
    settings: [
    {
      rangeFrom: ref<string | number>(''),
      rangeTo: ref<string | number>(''),
      mandatoryAssignees: ref<number>(0),
      saturdays: ref<boolean>(false),
      sundays: ref<boolean>(false),
      publicHolidays: ref<boolean>(false),
      startTime: ref<string | number>(''),
      endTime: ref<string | number>('')
    }
  ]
})

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const addSetting = () => {
  service.settings.push({
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
  service.settings.splice(index, 1)
}

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null

  await useAPI('/service/save', { method: 'POST', body: JSON.stringify(service) })
  .then(async (response) => {
    if (response.status.value === "error" && response.error?.value?.statusCode === 409) {
      errorMessage.value = t('service.exists')
    } else if (response.status.value === "error") {
      errorMessage.value = t('service.error')
    } else {
      successMessage.value = t('service.success')
      errorMessage.value = null

      service.servicename = ''
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
