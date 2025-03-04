<template>
  <div>
    <NavMenu />
  </div>
  <div>
    <h1>Welcome {{ sessionStore.user?.username }}</h1>
  </div>
</template>

<script setup lang="ts">
const sessionStore = useUserSessionStore()

definePageMeta({
    middleware: ['authenticated']
})
</script>