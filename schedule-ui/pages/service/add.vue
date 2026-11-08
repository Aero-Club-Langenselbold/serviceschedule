<template>
  <AppPage :title="$t('service.add')" back="/service/list" :back-label="$t('nav.service.list')" width="md">
    <UCard :ui="{ root: 'rounded-xl' }">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <UFormField :label="$t('service.name')">
          <UInput v-model="service.servicename" type="text" class="w-full" />
        </UFormField>
        <UCheckbox v-model="service.addAtImport" :label="$t('service.addAtImport')" />

        <ServiceSettings :settings="service.settings" editable @remove="removeSetting" />

        <UButton color="neutral" variant="outline" icon="i-lucide-plus" @click.prevent="addSetting">
          {{ $t('service.addSetting') }}
        </UButton>

        <UButton block size="lg" :label="$t('service.save')" type="submit" />
      </form>
    </UCard>
  </AppPage>
</template>

<script lang="ts" setup>
import type { Service } from '~/types/service'

const { t } = useI18n()

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
    if (response.error?.value?.statusCode === 409) {
      errorMessage.value = t('service.exists')
    } else if (response.error?.value) {
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
