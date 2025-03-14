<template>
  <UAlert v-if="errorMessage" :description="errorMessage" icon="i-lucide-x" color="error"/>
  <UAlert v-if="successMessage" :description="successMessage" icon="i-lucide-check"/>
</template>

<script lang="ts" setup>
const props = defineProps<{
  errorMessage: string | null
  successMessage: string | null
}>()
</script>