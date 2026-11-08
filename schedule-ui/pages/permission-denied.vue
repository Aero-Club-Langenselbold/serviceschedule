<template>
  <AppPage :title="$t('permissionDenied.title')" width="sm">
    <UAlert :description="$t('permissionDenied.description')" icon="i-lucide-shield-alert" color="error" variant="subtle" class="mb-6" />
    <UButton to="/" icon="i-lucide-house" color="neutral" variant="outline" :label="$t('permissionDenied.back')" />
  </AppPage>
</template>

<script lang="ts" setup>
definePageMeta({
  middleware: ['authenticated']
})
</script>
