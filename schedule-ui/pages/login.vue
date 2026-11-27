<template>
  <div class="flex justify-center px-4 py-10 sm:py-16">
    <UCard class="w-full max-w-md" :ui="{ root: 'rounded-2xl', body: 'sm:p-8' }">
      <h1 class="mb-6 text-2xl font-semibold tracking-tight text-highlighted">{{ $t('login.title') }}</h1>
      <!-- the choice is remembered on this device, a kiosk tablet keeps showing the PIN sign in -->
      <UTabs
        v-model="mode"
        :items="modes"
        :content="false"
        size="lg"
        class="mb-6"
        :ui="{ trigger: 'flex-1' }"
        data-login-mode
      />
      <form v-if="mode === 'kiosk'" class="space-y-5" data-kiosk-login @submit.prevent="onKioskSubmit">
        <Messages :errorMessage="loginError" :successMessage="successMessage" />
        <p class="text-sm text-muted">{{ $t('login.kiosk.hint') }}</p>
        <UFormField :label="$t('login.kiosk.username')">
          <UInput v-model="kioskCredentials.username" type="email" autocomplete="username" trailing-icon="i-lucide-at-sign" size="xl" class="w-full" data-kiosk-username />
        </UFormField>
        <UFormField :label="$t('login.kiosk.pin')">
          <UPinInput
            v-model="pin"
            :length="6"
            type="number"
            mask
            otp
            size="xl"
            :aria-label="$t('login.kiosk.pin')"
            data-kiosk-pin
            @complete="onKioskSubmit"
          />
        </UFormField>
        <UButton block size="xl" icon="i-lucide-tablet-smartphone" :label="$t('login.kiosk.submit')" type="submit" :loading="working" :disabled="!kioskComplete" />
      </form>
      <form v-else class="space-y-5" @submit.prevent="onSubmit">
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
import { KioskLoginError } from '~/stores/user'
import type { LoginMode } from '~/composables/useKiosk'

const { t } = useI18n()
const loginError = ref<string | null>(null)
const userStore = useUserStore()
const { login, loginKiosk } = userStore
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

// Kiosk mode: e-mail address and PIN only, no password, security question or Vereinsflieger 2FA code; the PIN is
// checked locally, so it works with and without the Vereinsflieger login.
const mode = ref<LoginMode>(storedLoginMode())
watch(mode, (value) => {
  storeLoginMode(value)
  loginError.value = null
})
const modes = computed(() => [
  { label: t('login.modes.regular'), value: 'regular', icon: 'i-lucide-log-in' },
  { label: t('login.modes.kiosk'), value: 'kiosk', icon: 'i-lucide-tablet-smartphone' }
])

const kioskCredentials = reactive({ username: '' })
// digits as the number pin input reports them; empty fields are undefined
const pin = ref<(number | undefined)[]>([])
const pinValue = computed(() => pin.value.map(digit => digit ?? '').join(''))
const kioskComplete = computed(() => !!kioskCredentials.username.trim() && /^\d{6}$/.test(pinValue.value))

const onKioskSubmit = async () => {
  if (!kioskComplete.value || working.value) {
    return
  }
  working.value = true
  try {
    await loginKiosk({ username: kioskCredentials.username.trim(), pin: pinValue.value })
    await navigateTo(KIOSK_HOME)
  } catch (error) {
    loginError.value = error instanceof KioskLoginError && error.reason === 'locked' ? t('login.kiosk.locked') : t('login.kiosk.invalid')
    pin.value = []
  } finally {
    working.value = false
  }
}

const working = ref(false)
const show = ref(false)
const successMessage = ref<string | null>(null)

watchEffect(() => {
  fetchData()
})
</script>


