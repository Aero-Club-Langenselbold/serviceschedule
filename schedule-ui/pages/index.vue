<template>
  <div>
    <NavMenu />
  </div>
  <div>
    <h1>Welcome {{ user?.username }}</h1>
  </div>
</template>

<script setup lang="ts">
const userStore = useUserStore()
const { user } = userStore

definePageMeta({
    middleware: ['authenticated']
})
</script>