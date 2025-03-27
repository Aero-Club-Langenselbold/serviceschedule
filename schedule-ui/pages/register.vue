<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-md border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('register.title') }}</h2>
      <form @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <div class="mb-4 mt-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('register.username') }}
          </label>
          <UInput v-model="register.username" type="email" trailing-icon="i-lucide-at-sign" :placeholder="$t('register.username')" class="w-full" @update:modelValue="checkPassword"/>
        </div>
        <div class="mb-4">
          <UFormField :label="$t('register.password')">
            <UInput v-model="register.password" :placeholder="$t('register.password')" :color="color" :type="show ? 'text' : 'password'" :ui="{ trailing: 'pe-1' }" :aria-invalid="score < 4" aria-describedby="password-strength" class="w-full" @update:modelValue="checkPassword">
              <template #trailing>
                <UButton color="neutral" variant="link" size="sm" :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'" :aria-label="show ? 'Hide password' : 'Show password'" :aria-pressed="show" aria-controls="password" @click="show = !show"/>
              </template>
            </UInput>
          </UFormField>

          <UProgress :color="color" :indicator="text" :model-value="score" :max="4" size="sm"/>

          <p id="password-strength" class="text-sm font-medium">
            {{ text }}. {{t('register.mustContain')}}
          </p>

          <ul class="space-y-1" aria-label="Password requirements">
            <li v-for="(req, index) in strength" :key="index" class="flex items-center gap-0.5" :class="req.met ? 'text-(--ui-success)' : 'text-(--ui-text-muted)'">
              <UIcon :name="req.met ? 'i-lucide-circle-check' : 'i-lucide-circle-x'" class="size-4 shrink-0" />

              <span class="text-xs font-light">
                {{ req.text }}
                <span class="sr-only">
                  {{ req.met ? ' - Requirement met' : ' - Requirement not met' }}
                </span>
              </span>
            </li>
          </ul>
        </div>
        <div class="mb-4">
          <UFormField :label="$t('register.confirmPassword')" :error="passwordError">
            <UInput v-model="register.confirmPassword" :placeholder="$t('register.password')" :type="show ? 'text' : 'password'" :ui="{ trailing: 'pe-1' }" class="w-full" @update:modelValue="checkPassword">
              <template #trailing>
                <UButton color="neutral" variant="link" size="sm" :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'" :aria-label="show ? 'Hide password' : 'Show password'" :aria-pressed="show" aria-controls="password" @click="show = !show"/>
              </template>
            </UInput>
          </UFormField>
        </div>
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('register.key') }}
          </label>
          <UInput v-model="register.registrationKey" type="text" trailing-icon="i-lucide-key" :placeholder="$t('register.key')" class="w-full" @update:modelValue="checkPassword"/>
        </div>
        <div class="flex items-center justify-between">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('register.submit')" type="submit" :disabled="!registerEnabled"/>
        </div>
      </form>
    </div>
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