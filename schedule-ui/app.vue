<template>
  <UApp>
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>

<script setup lang="ts">
const { setLocale } = useI18n()
const localeStore = useLocaleStore()
const { getUserLocale, validate } = localeStore
const featureStore = useFeatureStore()
const { fetchFeatures } = featureStore

await validate()
await fetchFeatures()
setLocale(getUserLocale())
</script>
