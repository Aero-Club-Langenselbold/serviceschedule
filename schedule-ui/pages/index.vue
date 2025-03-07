<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="text-center p-6 bg-white rounded-lg shadow-md max-w-lg break-words">
      <h1 class="text-4xl font-bold text-gray-800 mb-4">{{ $t('app.name') }}</h1>
      <p class="text-lg text-gray-600">{{ $t('app.welcome') }} {{ userData.firstname }} {{ userData.lastname }}</p>
      <p class="text-lg text-gray-600">{{ displayText }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const userStore = useUserStore()
const { user } = storeToRefs(userStore)
const { t } = useI18n()
const userData = ref({})

definePageMeta({
    middleware: ['authenticated']
})

// see who we got here
const displayText = ref('')
if (user.value?.roles.includes('ROLE_ADMIN')) {
    displayText.value = t('app.user.adminWelcome')
} else {
    const {data, error } = await useAPI('/participant/fetchByMail/' + user.value?.username, { method: 'GET' })
    userData.value = data.value
    if (error.value === "error" || data.value === '') {
      displayText.value = t('app.user.userWelcome')
    } else {
      displayText.value = t('app.user.participantWelcome')
    }
}
</script>