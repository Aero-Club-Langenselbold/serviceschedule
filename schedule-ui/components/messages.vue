<template>
  <div v-if="errorMessage" class="mb-4 p-4 text-red-700 bg-red-100 border border-red-400 rounded">
    {{ errorMessage }}
  </div>
  <div v-if="successMessage" class="mb-4 p-4 text-green-700 bg-green-100 border border-green-400 rounded">
    {{ successMessage }}
  </div>
</template>

<script lang="ts" setup>
const props = defineProps<{
  errorMessage: string | null
  successMessage: string | null
}>()
</script>