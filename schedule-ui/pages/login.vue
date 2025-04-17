<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-md border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('login.title') }}</h2>
      <form @submit.prevent="onSubmit">
        <Messages :errorMessage="loginError" :successMessage="successMessage" />
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('login.username') }}
          </label>
          <UInput v-model="credentials.username" type="email" trailing-icon="i-lucide-at-sign" :placeholder="$t('login.username')" class="w-full" />
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('login.password') }}
          </label>
          <UInput v-model="credentials.password" :placeholder="$t('login.password')"  :type="show ? 'text' : 'password'" :ui="{ trailing: 'pe-1' }" class="w-full" >
            <template #trailing>
              <UButton
                color="neutral"
                variant="link"
                size="sm"
                :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                :aria-label="show ? 'Hide password' : 'Show password'"
                :aria-pressed="show"
                aria-controls="password"
                @click="show = !show"
              />
            </template>
          </UInput>
        </div>
        <div class="mb-6 text-right">
          <ULink v-if="!features.some(feature => feature.id === 'vfLoginFeature')" to="#" >{{ $t('login.forgot') }}</ULink>
        </div>
        <div class="flex items-center justify-between">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('login.submit')" type="submit"/>
          <UButton
            v-if="!features.some(feature => feature.id === 'vfLoginFeature')"
            icon="i-lucide-rocket"
            color="neutral"
            size="md"
            :label="$t('login.register')"
            @click="navigateTo('/register')"
          />
        </div>
      </form>
    </div>
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
  password: ref<string | number>('')
})

const onSubmit = async () => {
  await login(credentials)
  .then(async () => {
    if (isLoggedIn.value) {
      await navigateTo('/')
    }
  })
  .catch(() => {
    loginError.value = t('error.credentials')
  })
}

const show = ref(false)
const { t } = useI18n()
const loginError = ref<string | null>(null)
const successMessage = ref<string | null>(null)
</script>


