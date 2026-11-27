<template>
  <div class="space-y-6">
    <div class="rounded-xl border border-default bg-default p-4" data-checklist-header>
      <div class="flex flex-wrap items-center gap-2">
        <UBadge :color="statusColor(summary.status)" variant="subtle" :label="statusLabel(summary.status)" data-checklist-status />
        <span class="text-sm text-muted">{{ formatDay(summary.date) }}</span>
      </div>
      <dl class="mt-3 space-y-1 text-sm">
        <div class="flex flex-wrap gap-x-1.5" data-created-by>
          <dt class="text-muted">{{ $t('operations.checklist.createdBy') }}:</dt>
          <dd class="font-medium text-highlighted">{{ summary.createdBy?.name ?? $t('operations.checklist.unknownUser') }}</dd>
          <dd class="text-muted">({{ formatWhen(summary.createdAt) }})</dd>
        </div>
        <div v-if="summary.submittedAt" class="flex flex-wrap gap-x-1.5" data-submitted-by>
          <dt class="text-muted">{{ $t('operations.checklist.submittedBy') }}:</dt>
          <dd class="font-medium text-highlighted">{{ summary.submittedBy?.name ?? $t('operations.checklist.unknownUser') }}</dd>
          <dd class="text-muted">({{ formatWhen(summary.submittedAt) }})</dd>
        </div>
        <div v-if="summary.approvedAt" class="flex flex-wrap gap-x-1.5" data-approved-by>
          <dt class="text-muted">{{ $t('operations.checklist.approvedBy') }}:</dt>
          <dd class="font-medium text-highlighted">{{ summary.approvedBy?.name ?? $t('operations.checklist.unknownUser') }}</dd>
          <dd class="text-muted">({{ formatWhen(summary.approvedAt) }})</dd>
        </div>
      </dl>
      <div class="mt-4 space-y-1">
        <div class="flex justify-between text-xs text-muted">
          <span>{{ $t('operations.checklist.progress', { checked: summary.checkedCount, total: summary.itemCount }) }}</span>
          <span v-if="summary.notOkCount" class="font-medium text-error">{{ $t('operations.checklist.notOkCount', summary.notOkCount) }}</span>
        </div>
        <UProgress :model-value="summary.checkedCount" :max="Math.max(summary.itemCount, 1)" size="sm" />
      </div>
    </div>

    <ol class="space-y-3">
      <li
        v-for="item in detail.items" :key="item.id" :data-item="item.position"
        class="rounded-xl border bg-default p-4"
        :class="item.result === 'NOT_OK' ? 'border-error/50' : item.result === 'OK' ? 'border-success/40' : 'border-default'"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <p class="min-w-0 font-medium text-highlighted">
            <span class="mr-1 text-muted">{{ item.position + 1 }}.</span>{{ item.text }}
          </p>
          <div class="flex shrink-0 gap-2">
            <UButton
              icon="i-lucide-check" :label="$t('operations.result.OK')" :disabled="!detail.editable || busy || !!item.defect"
              :color="item.result === 'OK' ? 'success' : 'neutral'" :variant="item.result === 'OK' ? 'solid' : 'outline'"
              :aria-pressed="item.result === 'OK'" data-result="OK" @click="toggle(item, 'OK')"
            />
            <UButton
              icon="i-lucide-x" :label="$t('operations.result.NOT_OK')" :disabled="!detail.editable || busy || !!item.defect"
              :color="item.result === 'NOT_OK' ? 'error' : 'neutral'" :variant="item.result === 'NOT_OK' ? 'solid' : 'outline'"
              :aria-pressed="item.result === 'NOT_OK'" data-result="NOT_OK" @click="toggle(item, 'NOT_OK')"
            />
          </div>
        </div>

        <div v-if="detail.editable" class="mt-3 flex flex-col gap-2 sm:flex-row sm:items-center">
          <UInput
            v-model="comments[item.id]" :placeholder="$t('operations.checklist.commentPlaceholder')" class="w-full"
            :aria-label="$t('operations.checklist.comment')" data-comment @blur="saveComment(item)" @keydown.enter.prevent="saveComment(item)"
          />
          <UButton
            v-if="!item.defect" icon="i-lucide-triangle-alert" color="error" variant="soft" class="shrink-0"
            :label="$t('operations.defect.report')" :disabled="busy" data-report-defect @click="openDefect(item)"
          />
        </div>
        <p v-else-if="item.comment" class="mt-2 text-sm text-default" data-comment-text>{{ item.comment }}</p>

        <div class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted">
          <span v-if="item.checkedBy">{{ $t('operations.checklist.checkedBy', { name: item.checkedBy.name, when: formatWhen(item.checkedAt) }) }}</span>
          <ULink
            v-if="item.defect" :to="{ path: '/maintenance/defects', query: { defect: item.defect.id } }"
            class="inline-flex items-center gap-1 font-medium text-error" data-defect-link
          >
            <UIcon name="i-lucide-triangle-alert" class="size-3.5" />
            {{ $t('operations.defect.linked', { title: item.defect.title }) }}
          </ULink>
        </div>
      </li>
    </ol>

    <div v-if="detail.submittable || detail.approvable || detail.reopenable || (detail.editable && !detail.submittable)" class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
      <p v-if="detail.editable && !detail.submittable" class="text-sm text-muted sm:mr-auto">{{ $t('operations.checklist.submitHint') }}</p>
      <UButton
        v-if="detail.reopenable" color="neutral" variant="outline" icon="i-lucide-undo-2" :label="$t('operations.checklist.reopen')"
        :loading="busy" data-reopen @click="act('reopen')"
      />
      <UButton
        v-if="detail.editable" icon="i-lucide-send" :label="$t('operations.checklist.submit')" :disabled="!detail.submittable"
        :loading="busy" data-submit @click="act('submit')"
      />
      <UButton
        v-if="detail.approvable" color="success" icon="i-lucide-badge-check" :label="$t('operations.checklist.approve')"
        :loading="busy" data-approve @click="act('approve')"
      />
    </div>

    <OperationsChecklistDefectForm v-model:open="defectOpen" :checklist-id="summary.id" :item="defectItem" @saved="emit('update', $event)" />
  </div>
</template>

<script lang="ts" setup>
import type { ChecklistDetail, ChecklistPoint, ChecklistItemResult } from '~/types/operations'

/** A checklist of a day: check items, comment, report defects, confirm complete; approve and send back for operations administrators. */
const props = defineProps<{ detail: ChecklistDetail }>()
const emit = defineEmits<{ update: [detail: ChecklistDetail] }>()

const { t } = useI18n()
const toast = useToast()
const { statusLabel, statusColor } = useOperations()
const { formatDay, formatWhen } = useMaintenance()

const summary = computed(() => props.detail.checklist)
const busy = ref(false)
const comments = reactive<Record<string, string>>({})
const defectOpen = ref(false)
const defectItem = ref<ChecklistPoint | null>(null)

watch(() => props.detail.items, (items) => {
  items.forEach((item) => { comments[item.id] = item.comment ?? '' })
}, { immediate: true })

const send = async (item: ChecklistPoint, result: ChecklistItemResult | null, comment: string | null) => {
  busy.value = true
  const { data, error } = await useAPI<ChecklistDetail>(`/operations/checklists/${summary.value.id}/items/${item.id}`,
    { method: 'PUT', body: { result, comment } })
  busy.value = false
  if (error.value) {
    toast.add({ title: t('operations.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  emit('update', data.value as ChecklistDetail)
}

// a second click on the chosen result unchecks the item
const toggle = (item: ChecklistPoint, result: ChecklistItemResult) =>
  send(item, item.result === result ? null : result, comments[item.id] ?? null)

const saveComment = (item: ChecklistPoint) => {
  const comment = (comments[item.id] ?? '').trim()
  if (comment !== (item.comment ?? '')) {
    send(item, item.result, comment || null)
  }
}

const openDefect = (item: ChecklistPoint) => {
  defectItem.value = item
  defectOpen.value = true
}

const act = async (action: 'submit' | 'approve' | 'reopen') => {
  busy.value = true
  const { data, error } = await useAPI<ChecklistDetail>(`/operations/checklists/${summary.value.id}/${action}`, { method: 'POST' })
  busy.value = false
  if (error.value) {
    toast.add({ title: t('operations.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  toast.add({ title: t(`operations.checklist.${action}Success`), icon: 'i-lucide-circle-check', color: 'primary' })
  emit('update', data.value as ChecklistDetail)
}
</script>
