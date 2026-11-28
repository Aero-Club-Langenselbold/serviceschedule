<template>
  <figure class="group relative overflow-hidden rounded-lg border border-default bg-elevated" :data-photo="photo.id">
    <button type="button" class="block aspect-square w-full" :aria-label="$t('winch.photos.open')" @click="open = true">
      <img v-if="url" :src="url" :alt="photo.filename ?? $t('winch.photos.photo')" class="size-full object-cover" loading="lazy">
      <span v-else class="flex size-full items-center justify-center text-muted">
        <UIcon :name="failed ? 'i-lucide-image-off' : 'i-lucide-image'" class="size-6" />
      </span>
    </button>
    <figcaption class="truncate px-2 py-1 text-xs text-muted">{{ photo.uploadedBy?.name ?? $t('winch.unknownUser') }} · {{ formatTime(photo.uploadedAt) }}</figcaption>
    <UButton
      v-if="photo.deletable" icon="i-lucide-trash-2" color="error" variant="solid" size="xs" class="absolute top-1 right-1"
      :aria-label="$t('winch.photos.delete')" data-delete-photo @click="confirmOpen = true"
    />

    <UModal v-model:open="open" :title="photo.filename ?? $t('winch.photos.photo')" :ui="{ content: 'sm:max-w-4xl' }">
      <template #body>
        <img v-if="url" :src="url" :alt="photo.filename ?? $t('winch.photos.photo')" class="mx-auto max-h-[75vh] w-auto rounded-lg">
        <p class="mt-2 text-sm text-muted">{{ $t('winch.photos.uploadedBy', { name: photo.uploadedBy?.name ?? $t('winch.unknownUser'), when: formatWhen(photo.uploadedAt) }) }}</p>
      </template>
    </UModal>

    <UModal v-model:open="confirmOpen" :title="$t('winch.photos.deleteConfirm')">
      <template #body>
        <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <UButton :label="$t('common.cancel')" color="neutral" variant="outline" @click="confirmOpen = false" />
          <UButton :label="$t('common.delete')" color="error" data-confirm-delete-photo @click="remove" />
        </div>
      </template>
    </UModal>
  </figure>
</template>

<script lang="ts" setup>
import type { WinchPhoto } from '~/types/winch'

/** A photo of a rope. The image needs the token, so it is loaded as blob instead of a plain image URL. */
const props = defineProps<{ photo: WinchPhoto }>()
const emit = defineEmits<{ delete: [photo: WinchPhoto] }>()

const { formatWhen } = useMaintenance()
const { locale } = useI18n()

const url = ref<string | null>(null)
const failed = ref(false)
const open = ref(false)
const confirmOpen = ref(false)

const formatTime = (timestamp: string) => new Date(timestamp).toLocaleTimeString(locale.value, { hour: '2-digit', minute: '2-digit' })

const load = async () => {
  const { data, error } = await useAPI<Blob>(`/maintenance/winch/photos/${props.photo.id}`, { method: 'GET', responseType: 'blob' })
  if (error.value || !(data.value instanceof Blob)) {
    failed.value = true
    return
  }
  try {
    url.value = URL.createObjectURL(data.value)
  } catch {
    failed.value = true
  }
}

const remove = () => {
  confirmOpen.value = false
  emit('delete', props.photo)
}

onMounted(load)
onBeforeUnmount(() => {
  if (url.value) {
    URL.revokeObjectURL(url.value)
  }
})
</script>
