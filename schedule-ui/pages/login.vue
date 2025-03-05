<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="bg-white p-8 rounded-4xl shadow-md w-full max-w-md">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('login.title') }}</h2>
      <form @submit.prevent="login">
        <div class="mb-4">
          <label class="block text-gray-700 text-sm font-bold mb-2">
            {{ $t('login.username') }}
          </label>
          <UInput v-model="credentials.username" type="email" trailing-icon="i-lucide-at-sign" :placeholder="$t('login.username')" />
        </div>
        <div class="mb-6">
          <label class="block text-gray-700 text-sm font-bold mb-2">
            {{ $t('login.password') }}
          </label>
          <UInput v-model="credentials.password" :placeholder="$t('login.password')"  :type="show ? 'text' : 'password'" :ui="{ trailing: 'pe-1' }">
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
          <ULink to="#">{{ $t('login.forgot') }}</ULink>
        </div>
        <div v-if="loginError" class="mb-4 text-red-500 text-sm">
          {{ loginError }}
        </div>
        <div class="flex items-center justify-between">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('login.submit')" type="submit"/>
          <UButton icon="i-lucide-rocket" color="neutral" size="md" :label="$t('login.register')" />
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
const show = ref(false)
const { t } = useI18n()
const { fetch: refreshSession } = useUserSession()
const loginError = ref<string | null>(null)
const credentials = reactive({
  username: ref<string | number>(''),
  password: ref<string | number>('')
})
async function login() {
  await useAPI('/auth/signin', {
    method: 'POST',
    body: credentials
  })
  .then(async (response) => {
    const r = (response.data.value as any)
    const userResponse = {
      user: {
        id: r.id,
        username: r.username,
        roles: r.roles
      },
      session: {
        token: r.token,
        type: r.type
      },
      loggedIn: true
    }
    // pretty dirty hack: send user response to the nuxt server to store in session
    $fetch('/api/login', {
      method: 'POST',
      body: userResponse
    })
    .then(async () => {
      // Refresh the session on client-side and redirect to the home page
      await refreshSession()
      await navigateTo('/')
    })
    .catch((error) => {
      console.error(error)
    })
  })
  .catch(() => {
    loginError.value = t('error.credentials')
  })
}
</script>


