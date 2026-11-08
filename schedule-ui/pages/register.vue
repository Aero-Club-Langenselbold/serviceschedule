<template>
  <div class="flex justify-center px-4 py-10 sm:py-16">
    <UCard class="w-full max-w-md" :ui="{ root: 'rounded-2xl', body: 'sm:p-8' }">
      <h1 class="mb-6 text-2xl font-semibold tracking-tight text-highlighted">{{ $t('register.title') }}</h1>
      <form class="space-y-5" @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <UFormField :label="$t('register.username')">
          <UInput v-model="register.username" type="email" autocomplete="username" trailing-icon="i-lucide-at-sign" class="w-full" @update:modelValue="checkPassword" />
        </UFormField>
        <div class="space-y-2">
          <UFormField :label="$t('register.password')">
            <UInput v-model="register.password" autocomplete="new-password" :color="color" :type="show ? 'text' : 'password'" :ui="{ trailing: 'pe-1' }" :aria-invalid="score < 4" aria-describedby="password-strength" class="w-full" @update:modelValue="checkPassword">
              <template #trailing>
                <UButton color="neutral" variant="link" size="sm" :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'" :aria-label="show ? $t('common.hidePassword') : $t('common.showPassword')" :aria-pressed="show" @click="show = !show" />
              </template>
            </UInput>
          </UFormField>

          <UProgress :color="color" :model-value="score" :max="4" size="sm" />

          <p id="password-strength" class="text-sm font-medium text-default">
            {{ text }}. {{ t('register.mustContain') }}
          </p>

          <ul class="grid gap-1 sm:grid-cols-2" :aria-label="t('register.mustContain')">
            <li v-for="(req, index) in strength" :key="index" class="flex items-center gap-1.5 text-xs" :class="req.met ? 'text-success' : 'text-muted'">
              <UIcon :name="req.met ? 'i-lucide-circle-check' : 'i-lucide-circle'" class="size-4 shrink-0" />
              <span>{{ req.text }}</span>
            </li>
          </ul>
        </div>
        <UFormField :label="$t('register.confirmPassword')" :error="passwordError">
          <UInput v-model="register.confirmPassword" autocomplete="new-password" :type="show ? 'text' : 'password'" class="w-full" @update:modelValue="checkPassword" />
        </UFormField>
        <UFormField :label="$t('register.key')">
          <UInput v-model="register.registrationKey" type="text" trailing-icon="i-lucide-key" class="w-full" @update:modelValue="checkPassword" />
        </UFormField>
        <UButton block size="lg" :label="$t('register.submit')" type="submit" :disabled="!registerEnabled" />
      </form>
    </UCard>
  </div>
</template>

<script lang="ts" setup>
const { t } = useI18n()
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const show = ref(false)
const passwordError = ref<string | boolean | undefined>(undefined)
const registerEnabled = ref<boolean>(false)

type Register = {
  username: string
  password: string
  registrationKey: string
  confirmPassword: string
}

const register = ref<Register>({
  username: '',
  password: '',
  registrationKey: '',
  confirmPassword: ''
})

const checkPassword = () => {
  if (register.value.password !== register.value.confirmPassword) {
    passwordError.value = t('register.passwordMismatch')
  } else if (score.value < 4) {
    passwordError.value = true
  } else {
    passwordError.value = undefined
  }
  registerEnabled.value = passwordError.value === undefined && register.value.username !== '' && register.value.password !== '' && register.value.registrationKey !== '' && register.value.confirmPassword !== ''
}

const onSubmit = async () => {
  try {
    const { data: registerData, error: registerError } = await useAPI('/auth/signup', { method: 'POST', body: JSON.stringify(register.value) })

    if (registerError.value && registerError.value?.statusCode !== 200) {
      errorMessage.value = registerError.value?.data.message
    } else {
      successMessage.value = t('register.success')
    }

  } catch (error) {
    errorMessage.value = t('register.error')
  }
}

function checkStrength(str: string) {
  const requirements = [
    { regex: /.{8,}/, text: t('register.numberCharacters') },
    { regex: /\d/, text: t('register.numberMandatory') },
    { regex: /[a-z]/, text: t('register.lowercaseMandatory') },
    { regex: /[A-Z]/, text: t('register.uppercaseMandatory') }
  ]

  return requirements.map(req => ({ met: req.regex.test(str), text: req.text }))
}

const strength = computed(() => checkStrength(register.value.password))
const score = computed(() => strength.value.filter(req => req.met).length)

const color = computed(() => {
  if (score.value === 0) return 'neutral'
  if (score.value <= 1) return 'error'
  if (score.value <= 2) return 'warning'
  if (score.value === 3) return 'warning'
  return 'success'
})

const text = computed(() => {
  if (score.value === 0) return t('register.enterPassword')
  if (score.value <= 2) return t('register.weakPassword')
  if (score.value === 3) return t('register.mediumPassword')
  return t('register.strongPassword')
})
</script>