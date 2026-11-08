<template>
  <UModal :title="modalTitle" v-model:open="open">
    <UTooltip :text="tooltip" :class="buttonClass">
      <UButton :label="buttonLabel" :color="color" :variant="variant" :icon="icon" :loading="loading" :aria-label="buttonLabel ? undefined : tooltip" />
    </UTooltip>

    <template #body>
      <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton
          :label="abortLabel"
          color="neutral"
          variant="outline"
          @click="close()"
        />
        <UButton
          :label="confirmLabel"
          color="error"
          variant="solid"
          @click="handleConfirm"
        />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
const open = ref(false)

const close = () => {
  open.value = false
}

const props = defineProps<{
  variant: "outline" | "solid" | "soft" | "link" | "ghost" | "subtle" | undefined
  color: "error" | "primary" | "secondary" | "neutral" | "success" | "warning" | "info" | undefined
  icon: string | undefined
  loading: boolean | undefined
  buttonLabel: string | undefined
  confirmLabel: string | undefined
  abortLabel: string | undefined
  tooltip: string | undefined
  buttonClass: string | undefined
  modalTitle: string | undefined
  onConfirm: () => void
}>()

const handleConfirm = () => {
  props.onConfirm()
  close()
}
</script>
