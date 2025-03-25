<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-8/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('participants.import') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <form @submit.prevent="upload" enctype="multipart/form-data">
        <UInput type="file" v-on:change="onFileChange" accept=".csv" class="mt-3 mr-4"/>
        <UButton variant="solid" color="primary" icon="i-lucide-save" @click="upload" :loading="saving" class="mr-4">
          {{ $t('participants.import') }}
        </UButton>
      </form>
    </div>
  </div>
</template>

<script lang="ts" setup>
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const saving = ref(false)
const file = ref<File | null>(null)

const { t } = useI18n()

const onFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target.files) {
    return
  }

  file.value = target.files[0]
}

const upload = async () => {
  saving.value = true
  try {
    if (!file.value) {
      errorMessage.value = 'Please select a file to upload'
      return
    }

    const formData = new FormData()
    formData.append('file', file.value)


    const { error } = await useAPI('/participant/import', {
      method: 'POST',
      body: formData
    })

    if (error.value) {
      errorMessage.value = 'An error occurred while uploading the file'
      return
    }
    errorMessage.value = null
    successMessage.value = 'File uploaded successfully'
  } catch (e) {
    errorMessage.value = 'An error occurred while uploading the file'
  } finally {
    saving.value = false
  }
}

definePageMeta({
    middleware: ['authenticated', 'admin']
})
</script>