<template>
  <UApp>
    <div class="flex flex-col min-h-screen">
      <NuxtPage />
    </div>
    <Footer></Footer>
  </UApp>
</template>

<script setup lang="ts">
const { setLocale } = useI18n()
const localeStore = useLocaleStore()
const { getUserLocale, validate } = localeStore

await validate()
setLocale(getUserLocale())
</script>
