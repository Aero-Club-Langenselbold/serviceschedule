<template>
  <AppPage
    :title="$t('maintenance.inventory.title')" :description="$t('maintenance.inventory.description')"
    back="/maintenance" :back-label="$t('nav.maintenance.overview')" :error-message="errorMessage"
  >
    <template v-if="canManageMaintenance" #actions>
      <UButton icon="i-lucide-plus" :label="$t('maintenance.inventory.add')" @click="openForm(null)" />
    </template>

    <UTabs v-model="kind" :items="tabs" :content="false" variant="link" class="mb-4" />

    <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
      <UInput v-model="search" icon="i-lucide-search" :placeholder="$t('maintenance.inventory.search')" class="sm:w-64" />
      <USelect v-if="kind === 'SPARE_PART'" v-model="typeFilter" :items="typeFilterItems" :aria-label="$t('maintenance.inventory.fits')" class="sm:w-52" />
      <USwitch v-model="shortOnly" :label="$t('maintenance.inventory.shortOnly')" class="sm:ml-auto" />
    </div>

    <p v-if="loaded && !visible.length" class="rounded-xl border border-dashed border-default bg-default p-6 text-center text-muted">
      {{ items.some(item => item.kind === kind) ? $t('maintenance.inventory.noMatch') : $t('maintenance.inventory.empty') }}
    </p>
    <ul v-else class="divide-y divide-default overflow-hidden rounded-xl border border-default bg-default">
      <li v-for="item in visible" :key="item.id" :data-item="item.id" class="flex flex-col gap-3 p-4 md:flex-row md:items-center">
        <div class="min-w-0 flex-1 space-y-1">
          <div class="flex flex-wrap items-center gap-2">
            <span class="font-medium text-highlighted">{{ item.name }}</span>
            <UBadge v-if="item.status !== 'OK'" :color="stockColor(item.status)" variant="subtle" size="sm">{{ $t(`maintenance.inventory.status.${item.status}`) }}</UBadge>
          </div>
          <p class="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted">
            <span v-if="item.location" class="inline-flex items-center gap-1"><UIcon name="i-lucide-map-pin" class="size-3.5" />{{ item.location }}</span>
            <span v-if="item.kind === 'SPARE_PART'" class="inline-flex items-center gap-1">
              <UIcon name="i-lucide-plane" class="size-3.5" />{{ item.aircraftTypes.length ? item.aircraftTypes.join(', ') : $t('maintenance.templates.allAircraft') }}
            </span>
            <span v-if="item.counting === 'COUNT' && item.minQuantity !== null" class="inline-flex items-center gap-1">
              <UIcon name="i-lucide-arrow-down-to-line" class="size-3.5" />{{ $t('maintenance.inventory.minimum', { stock: formatStock({ ...item, quantity: item.minQuantity }) }) }}
            </span>
          </p>
          <p v-if="item.comment" class="flex items-start gap-1 text-sm text-default">
            <UIcon name="i-lucide-message-square" class="mt-0.5 size-3.5 shrink-0 text-muted" /><span class="whitespace-pre-line">{{ item.comment }}</span>
          </p>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-2 md:flex-nowrap md:gap-4">
          <!-- Everybody counts the stock -->
          <div class="flex items-center gap-2">
            <template v-if="item.counting === 'COUNT'">
              <UInputNumber
                :model-value="item.quantity" :min="0" :max="1000000" class="w-36"
                :aria-label="$t('maintenance.inventory.countOf', { name: item.name })"
                @update:model-value="(value: number | null) => setQuantity(item, value ?? 0)"
              />
              <span class="max-w-16 truncate text-sm text-muted md:w-16">{{ item.unit }}</span>
            </template>
            <UFieldGroup v-else :aria-label="$t('maintenance.inventory.countOf', { name: item.name })">
              <UButton
                v-for="level in INVENTORY_LEVELS" :key="level" :label="levelName(level)" size="sm"
                :color="item.quantity === level ? levelColor(level) : 'neutral'" :variant="item.quantity === level ? 'solid' : 'outline'"
                :aria-pressed="item.quantity === level" class="w-18 justify-center sm:w-21" @click="setQuantity(item, level)"
              />
            </UFieldGroup>
          </div>

          <div class="ml-auto flex shrink-0 gap-1">
            <UButton color="neutral" variant="ghost" icon="i-lucide-history" :aria-label="$t('maintenance.task.history')" @click="openHistory(item)" />
            <template v-if="canManageMaintenance">
              <UButton color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="$t('maintenance.inventory.edit')" @click="openForm(item)" />
              <ConfirmationDialog
                variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
                :tooltip="$t('maintenance.inventory.delete')" :modal-title="$t('maintenance.inventory.deleteConfirm')"
                :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="() => remove(item)"
              />
            </template>
          </div>
        </div>
      </li>
    </ul>

    <MaintenanceInventoryForm v-model:open="formOpen" :item="edited" :kind="kind" :aircraft-types="aircraftTypes" @saved="onSaved" />
    <MaintenanceInventoryHistory v-model:open="historyOpen" :item="historyItem" />
  </AppPage>
</template>

<script lang="ts" setup>
import type { InventoryItem, InventoryKind } from '~/types/maintenance'

definePageMeta({
  middleware: ['authenticated']
})

const ALL_TYPES = 'all'
const SAVE_DELAY = 600 // ms; clicks on a stepper are sent as one count

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const toast = useToast()
const { canManageMaintenance } = storeToRefs(useUserStore())
const { stockColor, levelName, formatStock } = useMaintenance()

const items = ref<InventoryItem[]>([])
const aircraftTypes = ref<string[]>([])
const loaded = ref(false)
const errorMessage = ref<string | null>(null)
const search = ref('')
const typeFilter = ref(ALL_TYPES)
const shortOnly = ref(false)

// The tab lives in the URL, so it survives a reload.
const kind = computed<InventoryKind>({
  get: () => (route.query.kind === 'SPARE_PART' ? 'SPARE_PART' : 'CONSUMABLE'),
  set: (value: InventoryKind) => router.replace({ query: { ...route.query, kind: value === 'SPARE_PART' ? value : undefined } })
})
const tabs = computed(() => (['CONSUMABLE', 'SPARE_PART'] as const).map(value => ({
  label: t(`maintenance.inventory.kindsPlural.${value}`),
  value,
  icon: value === 'CONSUMABLE' ? 'i-lucide-spray-can' : 'i-lucide-cog',
  badge: items.value.filter(item => item.kind === value).length || undefined
})))

const typeFilterItems = computed(() => [
  { label: t('maintenance.inventory.allTypes'), value: ALL_TYPES },
  ...[...new Set([...aircraftTypes.value, ...items.value.flatMap(item => item.aircraftTypes)])].sort((a, b) => a.localeCompare(b)).map(type => ({ label: type, value: type }))
])

const visible = computed(() => {
  const query = search.value.trim().toLocaleLowerCase(locale.value)
  return items.value
    .filter(item => item.kind === kind.value)
    .filter(item => !query || item.name.toLocaleLowerCase(locale.value).includes(query) || item.location?.toLocaleLowerCase(locale.value).includes(query))
    .filter(item => kind.value !== 'SPARE_PART' || typeFilter.value === ALL_TYPES || !item.aircraftTypes.length || item.aircraftTypes.includes(typeFilter.value))
    .filter(item => !shortOnly.value || item.status !== 'OK')
})

const LEVEL_COLORS = { 2: 'success', 1: 'warning', 0: 'error' } as const
const levelColor = (level: number) => LEVEL_COLORS[level as 0 | 1 | 2]

// --- counting: shown at once, saved after a short pause

const timers = new Map<string, ReturnType<typeof setTimeout>>()

const setQuantity = (item: InventoryItem, quantity: number) => {
  item.quantity = quantity
  clearTimeout(timers.get(item.id))
  timers.set(item.id, setTimeout(() => saveQuantity(item), item.counting === 'LEVEL' ? 0 : SAVE_DELAY))
}

const saveQuantity = async (item: InventoryItem) => {
  timers.delete(item.id)
  const { data, error } = await useAPI<InventoryItem>(`/maintenance/inventory/${item.id}/quantity`, { method: 'PUT', body: { quantity: item.quantity } })
  if (error.value || !data.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    await fetchData()
    return
  }
  // a later click may already wait to be saved; keep its quantity
  const index = items.value.findIndex(entry => entry.id === item.id)
  if (index >= 0) {
    items.value[index] = timers.has(item.id) ? { ...data.value, quantity: items.value[index]!.quantity } : data.value
  }
}

onBeforeUnmount(() => {
  // save pending counts instead of dropping them
  for (const [id, timer] of timers) {
    clearTimeout(timer)
    const item = items.value.find(entry => entry.id === id)
    if (item) {
      void saveQuantity(item)
    }
  }
})

// --- administration

const formOpen = ref(false)
const edited = ref<InventoryItem | null>(null)
const historyOpen = ref(false)
const historyItem = ref<InventoryItem | null>(null)

const openForm = (item: InventoryItem | null) => {
  edited.value = item
  formOpen.value = true
}

const openHistory = (item: InventoryItem) => {
  historyItem.value = item
  historyOpen.value = true
}

const onSaved = async (saved: InventoryItem) => {
  toast.add({ title: t('maintenance.saved'), icon: 'i-lucide-circle-check', color: 'primary' })
  if (saved.kind !== kind.value) {
    kind.value = saved.kind
  }
  await fetchData()
}

const remove = async (item: InventoryItem) => {
  const { error } = await useAPI(`/maintenance/inventory/${item.id}`, { method: 'DELETE' })
  toast.add(error.value
    ? { title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' }
    : { title: t('maintenance.deleted'), icon: 'i-lucide-circle-check', color: 'primary' })
  await fetchData()
}

const fetchData = async () => {
  const [itemResult, typeResult] = await Promise.all([
    useAPI<InventoryItem[]>('/maintenance/inventory', { method: 'GET' }),
    useAPI<string[]>('/maintenance/aircraft/types', { method: 'GET' })
  ])
  errorMessage.value = itemResult.error.value ? t('maintenance.loadError') : null
  items.value = itemResult.data.value ?? []
  aircraftTypes.value = typeResult.data.value ?? []
  loaded.value = !itemResult.error.value
}

onMounted(fetchData)
</script>
