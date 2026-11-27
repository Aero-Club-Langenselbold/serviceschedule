<template>
  <UModal v-model:open="open" :title="task?.title ?? ''" :description="task?.aircraftLabel" :ui="{ content: 'sm:max-w-2xl' }">
    <template #body>
      <div v-if="!task" class="flex justify-center py-8">
        <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-muted" />
      </div>
      <div v-else class="space-y-5">
        <div class="flex flex-wrap items-center gap-2">
          <UBadge :color="statusColor(task.status)" variant="subtle">{{ statusLabel(task.status) }}</UBadge>
          <UBadge :color="levelColor(task.level)" variant="outline">{{ levelLabel(task.level) }}</UBadge>
          <UBadge v-if="task.fromTemplate" color="neutral" variant="outline" icon="i-lucide-copy">{{ $t('maintenance.task.fromTemplate') }}</UBadge>
          <div v-if="canManage" class="ml-auto flex gap-1">
            <UButton v-if="!locked" color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="$t('maintenance.task.edit')" @click="emit('edit', task)" />
            <ConfirmationDialog
              variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
              :tooltip="$t('maintenance.task.delete')" :modal-title="$t('maintenance.task.deleteConfirm')"
              :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="remove"
            />
          </div>
        </div>

        <dl class="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <div v-if="task.durationMinutes" class="rounded-lg border border-default p-3">
            <dt class="text-xs text-muted">{{ $t('maintenance.duration.name') }}</dt>
            <dd class="font-medium text-highlighted">{{ formatDuration(task.durationMinutes) }}</dd>
          </div>
          <div v-if="task.checklist.length" class="rounded-lg border border-default p-3">
            <dt class="text-xs text-muted">{{ $t('maintenance.task.checklist') }}</dt>
            <dd class="font-medium text-highlighted">{{ checkedCount }} / {{ task.checklist.length }}</dd>
          </div>
          <div v-if="task.deadline" class="rounded-lg border border-default p-3">
            <dt class="text-xs text-muted">{{ $t('maintenance.task.deadline') }}</dt>
            <dd class="font-medium text-warning">{{ formatDay(task.deadline) }}</dd>
          </div>
        </dl>

        <p v-if="task.description" class="whitespace-pre-line text-default">{{ task.description }}</p>

        <NuxtLink
          v-if="task.defect" :to="`/maintenance/defects?defect=${task.defect.id}`"
          class="flex items-center gap-3 rounded-lg border border-default bg-muted p-3 transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary"
        >
          <span class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <UIcon name="i-lucide-triangle-alert" class="size-5" />
          </span>
          <span class="min-w-0 flex-1">
            <span class="block text-xs text-muted">{{ $t('maintenance.task.fromDefect') }}</span>
            <span class="block truncate font-medium text-highlighted">{{ task.defect.title }}</span>
          </span>
          <UBadge :color="defectStatusColor({ status: task.defect.status, signedAt: null })" variant="subtle">
            {{ defectStatusLabel({ status: task.defect.status, signedAt: null }) }}
          </UBadge>
        </NuxtLink>

        <ul v-if="task.checklist.length" class="divide-y divide-default rounded-lg border border-default" :aria-label="$t('maintenance.task.checklist')">
          <li v-for="item in task.checklist" :key="item.id" class="px-3 py-2.5">
            <UCheckbox
              :model-value="item.done" :disabled="locked || busy" :label="item.text"
              :description="item.done && item.doneBy ? `${item.doneBy.name} · ${formatWhen(item.doneAt)}` : undefined"
              :ui="{ label: item.done ? 'text-muted line-through' : '' }"
              @update:model-value="(value: boolean | 'indeterminate') => toggle(item.id, value === true)"
            />
          </li>
        </ul>

        <UAlert v-if="locked" color="success" variant="subtle" icon="i-lucide-shield-check" :title="$t('maintenance.task.signedBy', { name: personName(task.signedBy), date: formatWhen(task.signedAt) })" :description="$t('maintenance.task.locked')" />

        <section class="space-y-3">
          <h3 class="text-sm font-semibold text-highlighted">{{ $t('maintenance.task.notes') }}</h3>
          <p v-if="!task.notes.length" class="text-sm text-muted">{{ $t('maintenance.task.noNotes') }}</p>
          <ul v-else class="space-y-2">
            <li v-for="note in task.notes" :key="note.id" class="flex gap-2 rounded-lg border border-default p-3 text-sm">
              <div class="min-w-0 flex-1">
                <p class="mb-1 text-xs text-muted">{{ personName(note.author) }} · {{ formatWhen(note.createdAt) }}</p>
                <p class="whitespace-pre-line text-default [overflow-wrap:anywhere]">{{ note.content }}</p>
              </div>
              <UButton v-if="note.deletable" color="neutral" variant="ghost" size="sm" icon="i-lucide-x" :aria-label="$t('maintenance.task.deleteNote')" @click="deleteNote(note.id)" />
            </li>
          </ul>
          <form v-if="!locked" class="space-y-2" @submit.prevent="addNote">
            <UTextarea v-model="noteDraft" :rows="2" autoresize class="w-full" :maxlength="5000" :placeholder="$t('maintenance.task.notePlaceholder')" :aria-label="$t('maintenance.task.notes')" />
            <div class="flex justify-end">
              <UButton type="submit" color="neutral" variant="outline" icon="i-lucide-message-square-plus" :label="$t('maintenance.task.addNote')" :loading="busy" :disabled="!noteDraft.trim()" />
            </div>
          </form>
        </section>

        <UCollapsible>
          <UButton color="neutral" variant="link" class="-ml-2.5" trailing-icon="i-lucide-chevron-down" :label="$t('maintenance.task.history')" />
          <template #content>
            <ol class="mt-2 space-y-1.5 border-l border-default pl-4 text-sm">
              <li v-for="entry in task.history" :key="entry.id">
                <span class="font-medium text-highlighted">{{ $t(`maintenance.task.actions.${entry.action}`) }}</span>
                <span class="text-muted"> · {{ personName(entry.actor) }} · {{ formatWhen(entry.createdAt) }}</span>
              </li>
            </ol>
          </template>
        </UCollapsible>
      </div>
    </template>

    <template v-if="task" #footer>
      <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-center">
        <p class="text-sm text-muted sm:mr-auto">
          <template v-if="task.status === 'DONE' && task.doneBy">{{ $t('maintenance.task.doneBy', { name: task.doneBy.name, date: formatWhen(task.doneAt) }) }}</template>
        </p>
        <div class="flex flex-col-reverse gap-3 sm:flex-row">
          <template v-if="locked">
            <UButton v-if="canManage" color="neutral" variant="outline" :label="$t('maintenance.task.unsign')" :loading="busy" @click="act('sign', 'DELETE')" />
            <UButton color="neutral" :label="$t('maintenance.task.close')" @click="open = false" />
          </template>
          <template v-else-if="task.status === 'OPEN'">
            <UButton icon="i-lucide-play" :label="$t('maintenance.task.start')" :loading="busy" @click="act('start')" />
          </template>
          <template v-else-if="task.status === 'IN_PROGRESS'">
            <UButton color="neutral" variant="outline" :label="$t('maintenance.task.reset')" :loading="busy" @click="act('reset')" />
            <UButton icon="i-lucide-check" :label="$t('maintenance.task.finish')" :loading="busy" @click="act('finish')" />
          </template>
          <template v-else>
            <UButton color="neutral" variant="outline" :label="$t('maintenance.task.reopen')" :loading="busy" @click="act('reopen')" />
            <UButton v-if="canManage" color="success" icon="i-lucide-shield-check" :label="$t('maintenance.task.sign')" :loading="busy" @click="act('sign')" />
          </template>
        </div>
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Person, TaskDetail } from '~/types/maintenance'

const props = defineProps<{ taskId: string | null }>()
const emit = defineEmits<{ changed: [], deleted: [], edit: [task: TaskDetail] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { canManageMaintenance: canManage } = storeToRefs(useUserStore())
const { levelLabel, statusLabel, levelColor, statusColor, defectStatusLabel, defectStatusColor, formatDuration, formatDay, formatWhen } = useMaintenance()

const task = ref<TaskDetail | null>(null)
const busy = ref(false)
const noteDraft = ref('')

const locked = computed(() => !!task.value?.signedAt)
const checkedCount = computed(() => task.value?.checklist.filter(item => item.done).length ?? 0)
const personName = (person: Person | null) => person?.name ?? t('maintenance.task.unknownUser')

const load = async () => {
  if (!props.taskId) {
    return
  }
  const { data, error } = await useAPI<TaskDetail>(`/maintenance/tasks/${props.taskId}`, { method: 'GET' })
  if (error.value) {
    toast.add({ title: t('maintenance.loadError'), icon: 'i-lucide-bomb', color: 'error' })
    open.value = false
    return
  }
  task.value = data.value as TaskDetail
}

watch([open, () => props.taskId], ([isOpen]) => {
  if (isOpen) {
    task.value = null
    noteDraft.value = ''
    load()
  }
}, { immediate: true })

/** Every change answers with the whole task, so the dialog always shows what the server stored. */
const send = async (path: string, method: 'POST' | 'PUT' | 'DELETE', body?: object) => {
  if (!task.value) {
    return false
  }
  busy.value = true
  const { data, error } = await useAPI<TaskDetail>(`/maintenance/tasks/${task.value.id}${path}`, { method, body })
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
  task.value = data.value as TaskDetail
  emit('changed')
  return true
}

const act = async (action: 'start' | 'reset' | 'finish' | 'reopen' | 'sign', method: 'POST' | 'DELETE' = 'POST') => {
  await send(`/${action}`, method)
}

const toggle = (itemId: string, done: boolean) => send(`/checklist/${itemId}`, 'PUT', { done })

const addNote = async () => {
  if (await send('/notes', 'POST', { content: noteDraft.value })) {
    noteDraft.value = ''
  }
}

const deleteNote = (noteId: string) => send(`/notes/${noteId}`, 'DELETE')

const remove = async () => {
  if (!task.value) {
    return
  }
  const { error } = await useAPI(`/maintenance/tasks/${task.value.id}`, { method: 'DELETE' })
  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  toast.add({ title: t('maintenance.deleted'), icon: 'i-lucide-circle-check', color: 'primary' })
  open.value = false
  emit('deleted')
}

defineExpose({ reload: load })
</script>
