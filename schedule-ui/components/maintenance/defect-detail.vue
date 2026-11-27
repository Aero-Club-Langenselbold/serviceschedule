<template>
  <UModal v-model:open="open" :title="defect?.title ?? ''" :description="defect?.aircraftLabel" :ui="{ content: 'sm:max-w-2xl' }">
    <template #body>
      <div v-if="!defect" class="flex justify-center py-8">
        <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-muted" />
      </div>
      <div v-else class="space-y-5">
        <div class="flex flex-wrap items-center gap-2">
          <UBadge :color="defectStatusColor(defect)" variant="subtle" data-defect-status>{{ defectStatusLabel(defect) }}</UBadge>
          <UBadge :color="priorityColor(defect.priority)" variant="outline">{{ priorityLabel(defect.priority) }}</UBadge>
          <UBadge v-if="defect.assignedToMe" color="primary" variant="soft" icon="i-lucide-user-check">{{ $t('maintenance.defect.assignedToMe') }}</UBadge>
          <div class="ml-auto flex gap-1">
            <UButton v-if="defect.editable" color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="$t('maintenance.defect.edit')" @click="emit('edit', defect)" />
            <ConfirmationDialog
              v-if="defect.deletable"
              variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
              :tooltip="$t('maintenance.defect.delete')" :modal-title="$t('maintenance.defect.deleteConfirm')"
              :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="remove"
            />
          </div>
        </div>

        <p v-if="defect.description" class="whitespace-pre-line text-default [overflow-wrap:anywhere]">{{ defect.description }}</p>

        <dl class="grid gap-3 sm:grid-cols-2">
          <div class="rounded-lg border border-default p-3">
            <dt class="text-xs text-muted">{{ $t('maintenance.defect.assignees') }}</dt>
            <dd class="font-medium text-highlighted" data-assignees>
              {{ defect.assignees.length ? defect.assignees.map(person => person.name).join(', ') : $t('maintenance.defect.unassigned') }}
            </dd>
          </div>
          <div class="rounded-lg border border-default p-3">
            <dt class="text-xs text-muted">{{ $t('maintenance.defect.aircraft') }}</dt>
            <dd class="flex items-center justify-between gap-2">
              <span class="truncate font-medium text-highlighted">{{ defect.aircraftLabel }}</span>
              <UButton :to="`/maintenance/${defect.aircraftId}`" color="neutral" variant="link" size="xs" trailing-icon="i-lucide-arrow-right" :label="$t('maintenance.defect.openBoard')" class="shrink-0" />
            </dd>
          </div>
        </dl>

        <div v-if="defect.task" class="flex items-center gap-3 rounded-lg border border-default bg-muted p-3">
          <UIcon name="i-lucide-square-kanban" class="size-5 shrink-0 text-primary" />
          <NuxtLink :to="`/maintenance/${defect.aircraftId}?task=${defect.task.id}`" class="min-w-0 flex-1 truncate font-medium text-highlighted hover:underline">
            {{ $t('maintenance.defect.task') }}: {{ defect.task.title }}
          </NuxtLink>
          <UBadge :color="statusColor(defect.task.status)" variant="subtle">{{ statusLabel(defect.task.status) }}</UBadge>
        </div>
        <UButton
          v-else-if="canManage && !locked && isDefectOpen(defect)"
          color="neutral" variant="outline" size="sm" icon="i-lucide-square-plus" :label="$t('maintenance.defect.createTask')"
          :to="`/maintenance/${defect.aircraftId}?newTaskFor=${defect.id}`"
        />

        <UAlert
          v-if="locked" color="success" variant="subtle" icon="i-lucide-shield-check"
          :title="$t('maintenance.defect.signedBy', { name: personName(defect.signedBy), date: formatWhen(defect.signedAt) })"
          :description="$t('maintenance.defect.locked')"
        />

        <!-- rejecting needs a reason -->
        <form v-if="rejecting" class="space-y-2 rounded-lg border border-default p-3" data-reject-form @submit.prevent="reject">
          <UFormField :label="$t('maintenance.defect.rejectReason')" :help="$t('maintenance.defect.rejectHint')" required>
            <UTextarea v-model="rejectReason" :rows="2" autoresize class="w-full" :maxlength="5000" autofocus />
          </UFormField>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="rejecting = false" />
            <UButton type="submit" color="neutral" icon="i-lucide-ban" :label="$t('maintenance.defect.reject')" :loading="busy" :disabled="!rejectReason.trim()" />
          </div>
        </form>

        <section class="space-y-3">
          <h3 class="text-sm font-semibold text-highlighted">{{ $t('maintenance.defect.activity') }}</h3>
          <ol class="space-y-2" data-activity>
            <!-- defects reported before the activity log existed -->
            <li v-if="!events.some(event => event.action === 'REPORTED')" class="text-sm text-muted">
              {{ $t('maintenance.defect.reportedBy', { name: personName(defect.reportedBy), date: formatWhen(defect.reportedAt) }) }}
            </li>
            <li v-for="event in events" :key="event.id" :data-event="event.action">
              <div v-if="event.action === 'COMMENTED' || event.content" class="flex gap-2 rounded-lg border border-default p-3 text-sm">
                <div class="min-w-0 flex-1">
                  <p class="mb-1 text-xs text-muted">
                    <span class="font-medium text-default">{{ personName(event.actor) }}</span>
                    <template v-if="event.action !== 'COMMENTED'"> {{ describe(event) }}</template>
                    · {{ formatWhen(event.createdAt) }}
                  </p>
                  <p class="whitespace-pre-line text-default [overflow-wrap:anywhere]">{{ event.content }}</p>
                </div>
                <UButton v-if="event.deletable" color="neutral" variant="ghost" size="sm" icon="i-lucide-x" :aria-label="$t('maintenance.defect.deleteComment')" @click="send(`/comments/${event.id}`, 'DELETE')" />
              </div>
              <p v-else class="flex items-center gap-2 px-1 text-sm text-muted">
                <UIcon :name="EVENT_ICONS[event.action]" class="size-4 shrink-0" />
                <span><span class="font-medium text-default">{{ personName(event.actor) }}</span> {{ describe(event) }} · {{ formatWhen(event.createdAt) }}</span>
              </p>
            </li>
          </ol>
          <form v-if="!locked" class="space-y-2" @submit.prevent="addComment">
            <UTextarea v-model="commentDraft" :rows="2" autoresize class="w-full" :maxlength="5000" :placeholder="$t('maintenance.defect.commentPlaceholder')" :aria-label="$t('maintenance.defect.addComment')" />
            <div class="flex justify-end">
              <UButton type="submit" color="neutral" variant="outline" icon="i-lucide-message-square-plus" :label="$t('maintenance.defect.addComment')" :loading="busy" :disabled="!commentDraft.trim()" />
            </div>
          </form>
        </section>
      </div>
    </template>

    <template v-if="defect" #footer>
      <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-center">
        <p class="text-sm text-muted sm:mr-auto">
          <template v-if="defect.doneBy && !locked">{{ $t(defect.status === 'REJECTED' ? 'maintenance.defect.rejectedBy' : 'maintenance.defect.doneBy', { name: defect.doneBy.name, date: formatWhen(defect.doneAt) }) }}</template>
        </p>
        <div class="flex flex-col-reverse gap-3 sm:flex-row sm:flex-wrap sm:justify-end">
          <template v-if="locked">
            <UButton v-if="canManage" color="neutral" variant="outline" :label="$t('maintenance.defect.unsign')" :loading="busy" @click="send('/sign', 'DELETE')" />
            <UButton color="neutral" :label="$t('maintenance.task.close')" @click="open = false" />
          </template>
          <template v-else-if="isDefectOpen(defect)">
            <UButton v-if="canManage && !rejecting" color="neutral" variant="outline" icon="i-lucide-ban" :label="$t('maintenance.defect.reject')" @click="startRejecting" />
            <UButton v-if="defect.status === 'OPEN'" color="neutral" variant="outline" icon="i-lucide-play" :label="$t('maintenance.defect.start')" :loading="busy" @click="setStatus('IN_PROGRESS')" />
            <UButton v-else color="neutral" variant="outline" icon="i-lucide-rotate-ccw" :label="$t('maintenance.defect.reopen')" :loading="busy" @click="setStatus('OPEN')" />
            <UButton icon="i-lucide-check" :label="$t('maintenance.defect.markDone')" :loading="busy" @click="setStatus('DONE')" />
          </template>
          <template v-else>
            <!-- a rejection is taken back by maintenance administrators only -->
            <UButton v-if="canManage || defect.status !== 'REJECTED'" color="neutral" variant="outline" icon="i-lucide-rotate-ccw" :label="$t('maintenance.defect.reopen')" :loading="busy" @click="setStatus('OPEN')" />
            <UButton v-if="canManage" color="success" icon="i-lucide-shield-check" :label="$t('maintenance.defect.sign')" :loading="busy" @click="send('/sign', 'POST')" />
          </template>
        </div>
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Defect, DefectDetail, DefectEvent, DefectEventAction, DefectStatus, Person } from '~/types/maintenance'

const EVENT_ICONS: Record<DefectEventAction, string> = {
  REPORTED: 'i-lucide-triangle-alert',
  UPDATED: 'i-lucide-pencil',
  COMMENTED: 'i-lucide-message-square',
  STARTED: 'i-lucide-play',
  DONE: 'i-lucide-check',
  REJECTED: 'i-lucide-ban',
  REOPENED: 'i-lucide-rotate-ccw',
  ASSIGNED: 'i-lucide-user-plus',
  UNASSIGNED: 'i-lucide-user-minus',
  SIGNED: 'i-lucide-shield-check',
  UNSIGNED: 'i-lucide-shield-off'
}

/** A defect of the defect book with its activity; everything the user may do with it (the server decides). */
const props = defineProps<{ defectId: string | null }>()
const emit = defineEmits<{ changed: [], deleted: [], edit: [defect: Defect] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { canManageMaintenance: canManage } = storeToRefs(useUserStore())
const { defectStatusLabel, defectStatusColor, isDefectOpen, priorityLabel, priorityColor, statusLabel, statusColor, formatWhen } = useMaintenance()

const detail = ref<DefectDetail | null>(null)
const defect = computed(() => detail.value?.defect ?? null)
const events = computed(() => detail.value?.events ?? [])
const locked = computed(() => !!defect.value?.signedAt)
const busy = ref(false)
const commentDraft = ref('')
const rejecting = ref(false)
const rejectReason = ref('')

const personName = (person: Person | null) => person?.name ?? t('maintenance.task.unknownUser')
const describe = (event: DefectEvent) => t(`maintenance.defect.actions.${event.action}`, { name: personName(event.subject) })

const load = async () => {
  if (!props.defectId) {
    return
  }
  const { data, error } = await useAPI<DefectDetail>(`/maintenance/defects/${props.defectId}`, { method: 'GET' })
  if (error.value) {
    toast.add({ title: t(error.value.statusCode === 404 ? 'maintenance.defect.notFound' : 'maintenance.loadError'), icon: 'i-lucide-bomb', color: 'error' })
    open.value = false
    return
  }
  detail.value = data.value as DefectDetail
}

watch([open, () => props.defectId], ([isOpen]) => {
  if (isOpen) {
    detail.value = null
    commentDraft.value = ''
    rejecting.value = false
    load()
  }
}, { immediate: true })

/** Every change answers with the whole defect, so the dialog always shows what the server stored. */
const send = async (path: string, method: 'POST' | 'PUT' | 'DELETE', body?: object) => {
  if (!defect.value) {
    return false
  }
  busy.value = true
  const { data, error } = await useAPI<DefectDetail>(`/maintenance/defects/${defect.value.id}${path}`, { method, body })
  busy.value = false

  if (error.value) {
    const conflict = error.value.statusCode === 409
    toast.add({ title: t(conflict ? 'maintenance.conflict' : 'maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    if (conflict) {
      await load()
      emit('changed')
    }
    return false
  }
  detail.value = data.value as DefectDetail
  emit('changed')
  return true
}

const setStatus = (status: DefectStatus, comment?: string) => send('/status', 'PUT', { status, comment: comment ?? null })

const startRejecting = () => {
  rejectReason.value = ''
  rejecting.value = true
}

const reject = async () => {
  if (await setStatus('REJECTED', rejectReason.value)) {
    rejecting.value = false
  }
}

const addComment = async () => {
  if (await send('/comments', 'POST', { content: commentDraft.value })) {
    commentDraft.value = ''
  }
}

const remove = async () => {
  if (!defect.value) {
    return
  }
  const { error } = await useAPI(`/maintenance/defects/${defect.value.id}`, { method: 'DELETE' })
  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  toast.add({ title: t('maintenance.deleted'), icon: 'i-lucide-circle-check', color: 'primary' })
  open.value = false
  emit('deleted')
}

/** After editing elsewhere (the form), the parent shows the stored state. */
const show = (updated: DefectDetail) => {
  detail.value = updated
}

defineExpose({ reload: load, show })
</script>
