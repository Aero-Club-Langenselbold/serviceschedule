<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-md border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('participants.add') }}</h2>
      <form @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('service.name') }}
          </label>
          <UInput v-model="service.servicename" type="text" :placeholder="$t('service.name')" class="w-full" />
        </div>
        <div class="flex items-center justify-between">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('service.save')" type="submit" />
        </div>
      </form>
    </div>
  </div>
</template>

<script lang="ts" setup>
const { t } = useI18n()

const service = reactive({
  servicename: ref<string | number>('')
})

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

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
