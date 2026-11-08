<template>
  <div class="mx-auto w-full px-4 py-6 sm:px-6 sm:py-8" :class="widthClass">
    <UButton
      v-if="back"
      :to="back"
      :label="backLabel"
      icon="i-lucide-arrow-left"
      color="neutral"
      variant="link"
      class="mb-2 -ml-2.5"
    />
    <header class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
      <div class="min-w-0">
        <h1 class="text-2xl font-semibold tracking-tight text-highlighted">{{ title }}</h1>
        <p v-if="description" class="mt-1 text-sm text-muted">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="flex shrink-0 flex-wrap items-center gap-2">
        <slot name="actions" />
      </div>
    </header>
    <Messages :error-message="errorMessage ?? null" :success-message="successMessage ?? null" class="mb-6" />
    <slot />
  </div>
</template>

<script lang="ts" setup>
const props = withDefaults(defineProps<{
  title: string
  description?: string
  back?: string
  backLabel?: string
  errorMessage?: string | null
  successMessage?: string | null
  width?: 'sm' | 'md' | 'lg' | 'full'
}>(), { width: 'lg' })

const widthClass = computed(() => ({
  sm: 'max-w-lg',
  md: 'max-w-3xl',
  lg: 'max-w-6xl',
  full: 'max-w-screen-2xl'
})[props.width])
</script>
