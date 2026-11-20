<template>
  <UModal v-model:open="open" :title="item ? $t('maintenance.inventory.historyOf', { name: item.name }) : $t('maintenance.task.history')">
    <template #body>
      <p v-if="loading" class="text-sm text-muted">{{ $t('maintenance.inventory.loadingHistory') }}</p>
      <p v-else-if="!entries.length" class="text-sm text-muted">{{ $t('maintenance.inventory.noHistory') }}</p>
      <ol v-else class="space-y-3">
        <li v-for="entry in entries" :key="entry.id" class="flex gap-3 text-sm">
          <UIcon :name="icons[entry.action]" class="mt-0.5 size-4 shrink-0 text-muted" />
          <div class="min-w-0">
            <p class="text-default">
              <span class="font-medium text-highlighted">{{ $t(`maintenance.inventory.actions.${entry.action}`) }}</span>
              <template v-if="item && entry.quantity !== null"> · {{ formatStock({ ...item, quantity: entry.quantity }) }}</template>
            </p>
            <p class="text-xs text-muted">{{ entry.actor?.name ?? $t('maintenance.task.unknownUser') }} · {{ formatWhen(entry.createdAt) }}</p>
          </div>
        </li>
      </ol>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { InventoryHistoryAction, InventoryHistoryEntry, InventoryItem } from '~/types/maintenance'

const props = defineProps<{ item: InventoryItem | null }>()
const open = defineModel<boolean>('open', { default: false })

const { formatStock, formatWhen } = useMaintenance()

const entries = ref<InventoryHistoryEntry[]>([])
const loading = ref(false)
const icons: Record<InventoryHistoryAction, string> = { CREATED: 'i-lucide-plus', UPDATED: 'i-lucide-pencil', COUNTED: 'i-lucide-hash' }

watch(open, async (isOpen) => {
  if (!isOpen || !props.item) {
    return
  }
  loading.value = true
  entries.value = []
  const { data } = await useAPI<InventoryHistoryEntry[]>(`/maintenance/inventory/${props.item.id}/history`, { method: 'GET' })
  entries.value = data.value ?? []
  loading.value = false
}, { immediate: true })
</script>
