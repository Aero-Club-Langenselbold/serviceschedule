<template>
  <UModal
    :title="modalTitle"
    :close="{
      color: 'primary',
      variant: 'outline',
      class: 'rounded-full'
    }"
    v-model:open="open"
  >
    <UTooltip :text="tooltip" :class="buttonClass">
      <UButton :label="buttonLabel" :color="color" :variant="variant" :icon="icon" :loading="loading"/>
    </UTooltip>

    <template #body>
      <div class="flex justify-end gap-4">
        <UButton
          :label="confirmLabel"
          color="success"
          variant="solid"
          @click="handleConfirm"
        />
        <UButton
          :label="abortLabel"
          color="error"
          variant="outline"
          @click="close()"
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
