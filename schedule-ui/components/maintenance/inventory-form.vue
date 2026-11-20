<template>
  <UModal v-model:open="open" :title="item ? $t('maintenance.inventory.edit') : $t('maintenance.inventory.add')" :ui="{ content: 'sm:max-w-2xl' }">
    <template #body>
      <form id="inventory-form" class="space-y-5" @submit.prevent="save">
        <UFormField :label="$t('maintenance.inventory.name')" required>
          <UInput v-model="form.name" :placeholder="$t('maintenance.inventory.namePlaceholder')" maxlength="255" class="w-full" autofocus />
        </UFormField>
        <UFormField :label="$t('maintenance.inventory.kind')">
          <URadioGroup v-model="form.kind" orientation="horizontal" variant="table" :items="kindItems" />
        </UFormField>
        <div class="grid gap-4 sm:grid-cols-2">
          <UFormField :label="$t('maintenance.inventory.location')">
            <UInput v-model="location" :placeholder="$t('maintenance.inventory.locationPlaceholder')" maxlength="255" class="w-full" />
          </UFormField>
          <UFormField v-if="form.kind === 'SPARE_PART'" :label="$t('maintenance.inventory.fits')" :hint="form.aircraftTypes.length ? undefined : $t('maintenance.templates.allAircraft')">
            <USelectMenu v-model="form.aircraftTypes" :items="typeItems" multiple create-item class="w-full" :placeholder="$t('maintenance.templates.allAircraft')" @create="addType" />
          </UFormField>
        </div>
        <UFormField :label="$t('maintenance.inventory.counting')">
          <URadioGroup v-model="counting" orientation="horizontal" variant="table" :items="countingItems" />
        </UFormField>
        <div v-if="form.counting === 'COUNT'" class="grid gap-4 sm:grid-cols-3">
          <UFormField :label="$t('maintenance.inventory.quantity')">
            <UInputNumber v-model="form.quantity" :min="0" :max="1000000" class="w-full" />
          </UFormField>
          <UFormField :label="$t('maintenance.inventory.unit')">
            <UInput v-model="unit" :placeholder="$t('maintenance.inventory.unitPlaceholder')" maxlength="30" class="w-full" />
          </UFormField>
          <UFormField :label="$t('maintenance.inventory.minQuantity')" :help="$t('maintenance.inventory.minQuantityHint')">
            <UInputNumber v-model="minQuantity" :min="0" :max="1000000" class="w-full" />
          </UFormField>
        </div>
        <UFormField v-else :label="$t('maintenance.inventory.quantity')">
          <URadioGroup v-model="form.quantity" orientation="horizontal" variant="table" :items="levelItems" />
        </UFormField>
        <UFormField :label="$t('maintenance.inventory.comment')" :help="$t('maintenance.inventory.commentHint')">
          <UTextarea v-model="comment" :rows="2" autoresize maxlength="5000" class="w-full" />
        </UFormField>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="inventory-form" :label="$t('common.save')" :loading="saving" :disabled="!form.name.trim()" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { InventoryCounting, InventoryItem, InventoryItemInput, InventoryKind } from '~/types/maintenance'

const props = defineProps<{ item?: InventoryItem | null, aircraftTypes: string[], kind?: InventoryKind }>()
const emit = defineEmits<{ saved: [item: InventoryItem] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { levelName } = useMaintenance()

const empty = (): InventoryItemInput => ({
  name: '', kind: props.kind ?? 'CONSUMABLE', location: null, counting: 'COUNT', quantity: 0,
  minQuantity: null, unit: null, comment: null, aircraftTypes: []
})
const form = ref<InventoryItemInput>(empty())
const location = ref('')
const unit = ref('')
const comment = ref('')
// UInputNumber clears to null; null means "no minimum"
const minQuantity = ref<number | null>(null)
const createdTypes = ref<string[]>([])
const saving = ref(false)

const kindItems = computed(() => (['CONSUMABLE', 'SPARE_PART'] as const).map(value => ({ label: t(`maintenance.inventory.kinds.${value}`), value })))
const countingItems = computed(() => (['COUNT', 'LEVEL'] as const).map(value => ({ label: t(`maintenance.inventory.countings.${value}`), value })))
const levelItems = computed(() => INVENTORY_LEVELS.map(value => ({ label: levelName(value), value })))
const typeItems = computed(() => [...new Set([...props.aircraftTypes, ...createdTypes.value, ...form.value.aircraftTypes])].sort((a, b) => a.localeCompare(b)))

// Switching the counting starts with a sensible stock: "enough" for a level, nothing for a count.
const counting = computed({
  get: () => form.value.counting,
  set: (value: InventoryCounting) => {
    if (value !== form.value.counting) {
      form.value.counting = value
      form.value.quantity = value === 'LEVEL' ? 2 : 0
    }
  }
})

watch(open, (isOpen) => {
  if (isOpen) {
    form.value = props.item ? { ...props.item, aircraftTypes: [...props.item.aircraftTypes] } : empty()
    location.value = props.item?.location ?? ''
    unit.value = props.item?.unit ?? ''
    comment.value = props.item?.comment ?? ''
    minQuantity.value = props.item?.minQuantity ?? null
    createdTypes.value = []
  }
}, { immediate: true })

const addType = (type: string) => {
  const trimmed = type.trim()
  if (trimmed && !form.value.aircraftTypes.includes(trimmed)) {
    createdTypes.value.push(trimmed)
    form.value.aircraftTypes.push(trimmed)
  }
}

const save = async () => {
  if (!form.value.name.trim()) {
    return
  }
  saving.value = true
  const count = form.value.counting === 'COUNT'
  const body = {
    ...form.value,
    quantity: form.value.quantity ?? 0,
    location: location.value.trim() || null,
    unit: count ? unit.value.trim() || null : null,
    minQuantity: count ? minQuantity.value : null,
    comment: comment.value.trim() || null,
    aircraftTypes: form.value.kind === 'SPARE_PART' ? form.value.aircraftTypes : []
  }
  const { data, error } = props.item
    ? await useAPI<InventoryItem>(`/maintenance/inventory/${props.item.id}`, { method: 'PUT', body })
    : await useAPI<InventoryItem>('/maintenance/inventory', { method: 'POST', body })
  saving.value = false

  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('saved', data.value as InventoryItem)
}
</script>
