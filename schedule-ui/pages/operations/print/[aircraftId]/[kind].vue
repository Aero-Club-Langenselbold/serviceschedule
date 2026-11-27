<template>
  <div class="min-h-dvh bg-default text-default">
    <div class="mx-auto flex max-w-2xl flex-col items-center px-4 py-10 text-center print:py-0">
      <div class="mb-8 flex w-full flex-wrap justify-center gap-2 print:hidden">
        <UButton icon="i-lucide-printer" :label="$t('operations.print.print')" :disabled="!svg" data-print-button @click="print" />
        <UButton to="/operations" color="neutral" variant="outline" icon="i-lucide-arrow-left" :label="$t('operations.overview.title')" />
      </div>
      <p v-if="errorMessage" class="text-error">{{ errorMessage }}</p>
      <template v-else-if="today && kind">
        <h1 class="text-5xl font-bold tracking-tight text-highlighted" data-print-title>{{ today.registration }}</h1>
        <p class="mt-3 text-2xl text-muted">{{ kindLabel(kind) }}</p>
        <!-- eslint-disable-next-line vue/no-v-html -- SVG generated locally from our own URL -->
        <div class="mt-10 w-full max-w-md [&>svg]:h-auto [&>svg]:w-full" data-qr v-html="svg" />
        <p class="mt-6 text-sm text-muted">{{ $t('operations.print.hint') }}</p>
      </template>
    </div>
  </div>
</template>

<script lang="ts" setup>
import QRCode from 'qrcode'
import type { TodayChecklist } from '~/types/operations'

/**
 * Print view of the QR code of an aircraft and kind: registration as heading, centered QR code below. The code holds the
 * fixed link that always opens the checklist of the current day, so the printout never has to be replaced.
 */
const route = useRoute()
const { t } = useI18n()
const { kindLabel, todayUrl } = useOperations()

const kind = computed(() => parseKind(route.params.kind))
const today = ref<TodayChecklist | null>(null)
const svg = ref('')
const errorMessage = ref<string | null>(null)

const print = () => window.print()

onMounted(async () => {
  if (!kind.value) {
    errorMessage.value = t('operations.notFound')
    return
  }
  const aircraftId = String(route.params.aircraftId)
  const { data, error } = await useAPI<TodayChecklist>(`/operations/aircraft/${aircraftId}/checklists/${kind.value}/today`, { method: 'GET' })
  if (error.value) {
    errorMessage.value = error.value.statusCode === 404 ? t('operations.notFound') : t('operations.loadError')
    return
  }
  today.value = data.value as TodayChecklist
  svg.value = await QRCode.toString(todayUrl(aircraftId, kind.value), { type: 'svg', margin: 1, errorCorrectionLevel: 'M' })
})

definePageMeta({
  layout: false,
  middleware: ['authenticated']
})
</script>
