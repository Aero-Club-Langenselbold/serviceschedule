<template>
  <div>
    <NavMenu />
  </div>
  <div>
    <h1>Welcome {{ user?.username }}</h1>
  </div>
</template>

<script setup lang="ts">
const { loggedIn, user, fetch: refreshSession } = useUserSession()

definePageMeta({
    middleware: ['authenticated']
})
</script>