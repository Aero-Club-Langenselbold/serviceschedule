<template>
  <div v-if="errorMessage || successMessage" class="space-y-2">
    <UAlert v-if="errorMessage" :description="errorMessage" icon="i-lucide-circle-alert" color="error" variant="subtle" role="alert" />
    <UAlert v-if="successMessage" :description="successMessage" icon="i-lucide-circle-check" color="success" variant="subtle" role="status" />
  </div>
</template>

<script lang="ts" setup>
const props = defineProps<{
  errorMessage: string | null
  successMessage: string | null
}>()
</script>
