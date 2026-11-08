<template>
  <div class="flex justify-center px-4 py-10 sm:py-16">
    <UCard class="w-full max-w-md" :ui="{ root: 'rounded-2xl', body: 'sm:p-8' }">
      <h1 class="mb-6 text-2xl font-semibold tracking-tight text-highlighted">{{ $t('login.title') }}</h1>
      <form class="space-y-5" @submit.prevent="onSubmit">
        <Messages :errorMessage="loginError" :successMessage="successMessage" />
        <UFormField :label="$t('login.username')">
          <UInput v-model="credentials.username" type="email" autocomplete="username" trailing-icon="i-lucide-at-sign" class="w-full" />
        </UFormField>
        <UFormField :label="$t('login.password')">
          <UInput v-model="credentials.password" autocomplete="current-password" :type="show ? 'text' : 'password'" :ui="{ trailing: 'pe-1' }" class="w-full">
            <template #trailing>
              <UButton
                color="neutral"
                variant="link"
                size="sm"
                :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                :aria-label="show ? $t('common.hidePassword') : $t('common.showPassword')"
                :aria-pressed="show"
                @click="show = !show"
              />
            </template>
          </UInput>
        </UFormField>
        <UFormField v-if="features.some(feature => feature.id === 'securityQuestionFeature')" :label="securityQuestion ?? ''" :aria-label="$t('login.securityAnswer')">
          <UInput v-model="credentials.securityAnswer" type="text" trailing-icon="i-lucide-key" :aria-label="$t('login.securityAnswer')" class="w-full" />
        </UFormField>
        <UFormField v-if="features.some(feature => feature.id === 'vfLoginFeature')" :label="$t('login.authSecret')">
          <UInput v-model="credentials.authSecret" type="text" inputmode="numeric" autocomplete="one-time-code" trailing-icon="i-lucide-key" class="w-full" />
        </UFormField>
        <div v-if="!features.some(feature => feature.id === 'vfLoginFeature')" class="text-right text-sm">
          <ULink to="#">{{ $t('login.forgot') }}</ULink>
        </div>
        <UButton block size="lg" :label="$t('login.submit')" type="submit" :loading="working" />
        <UButton
          v-if="!features.some(feature => feature.id === 'vfLoginFeature')"
          block
          size="lg"
          icon="i-lucide-user-plus"
          color="neutral"
          variant="outline"
          :label="$t('login.register')"
          to="/register"
        />
      </form>
    </UCard>
  </div>
</template>

<script setup lang="ts">
const userStore = useUserStore()
const { login } = userStore
const featureStore = useFeatureStore()
const { validateFeatures, features } = featureStore
const { isLoggedIn } = storeToRefs(userStore)

await validateFeatures()

const credentials = reactive({
  username: ref<string | number>(''),
  password: ref<string | number>(''),
  authSecret: ref<string | number>(''),
  securityAnswer: ref<string | number>('')
})

const securityQuestion = ref<string | null>(null)

const fetchData = async () => {
  if (features.some(feature => feature.id === 'securityQuestionFeature')) {
    const { data: settingData, error: settingError } = await useAPI<any>('/settings/common-setting/security_feature_question', { method: 'GET' })
    if (settingError.value) {
      loginError.value = 'Error fetching settings data'
    } else {
      securityQuestion.value = settingData.value?.settingValue
    }
  }
}

const onSubmit = async () => {
  working.value = true
  await login(credentials)
  .then(async () => {
    if (isLoggedIn.value) {
      await navigateTo('/')
    }
  })
  .catch((error) => {
    // Log the error
    console.error('Login error:', error)
    loginError.value = t('error.credentials')
  })
  working.value = false
}

const working = ref(false)
const show = ref(false)
const { t } = useI18n()
const loginError = ref<string | null>(null)
const successMessage = ref<string | null>(null)

watchEffect(() => {
  fetchData()
})
</script>


