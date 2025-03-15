<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-8/12">
      <UAlert
      :title="$t('permissionDenied.title')"
      :description="$t('permissionDenied.description')"
      color="error"
      variant="outline"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
definePageMeta({
  middleware: ['authenticated']
})
</script>
