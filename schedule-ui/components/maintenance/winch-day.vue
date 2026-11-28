<template>
  <div class="space-y-6">
    <div class="rounded-xl border border-default bg-default p-4" data-winch-header>
      <div class="flex flex-wrap items-center gap-2">
        <UBadge v-if="detail.today" color="primary" variant="subtle" :label="$t('winch.today')" />
        <span class="font-medium text-highlighted">{{ formatDay(detail.date) }}</span>
      </div>
      <p class="mt-2 text-sm" data-drivers>
        <span class="text-muted">{{ $t('winch.drivers') }}:</span>
        <span class="ml-1 font-medium text-highlighted">{{ detail.drivers.length ? detail.drivers.map(driver => driver.name).join(', ') : $t('winch.noDrivers') }}</span>
      </p>
      <p v-if="detail.editable" class="mt-1 text-xs text-muted">{{ $t('winch.recordHint') }}</p>
    </div>

    <p v-if="!detail.ropes.length" class="text-sm text-muted" data-no-ropes>{{ detail.editable ? $t('winch.noRopesConfigured') : $t('winch.noRopeEntries') }}</p>

    <!-- one card per rope, side by side from tablet width on -->
    <div v-if="detail.ropes.length" class="grid gap-4 md:grid-cols-2" :class="{ 'xl:grid-cols-3': detail.ropes.length > 2 }" data-ropes>
      <section
        v-for="entry in detail.ropes" :key="entry.rope.id" class="flex min-w-0 flex-col rounded-xl border border-default bg-default p-4"
        :data-rope="entry.rope.id"
      >
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 class="flex items-center gap-2 text-lg font-semibold text-highlighted">
            <UIcon name="i-lucide-cable" class="size-5 text-primary" />
            {{ entry.rope.name }}
            <UBadge v-if="!entry.rope.active" color="neutral" variant="subtle" size="sm" :label="$t('winch.inactive')" />
          </h2>
          <div v-if="countsOf(entry).length" class="flex flex-wrap gap-1" data-incident-counts>
            <UBadge v-for="count in countsOf(entry)" :key="count.name" color="warning" variant="subtle" :label="`${count.name} × ${count.count}`" />
          </div>
        </div>
  
        <!-- record an incident: one button per kind -->
        <div v-if="detail.editable && entry.rope.active && detail.incidentTypes.length" class="mt-4">
          <p class="mb-2 text-sm text-muted">{{ $t('winch.addIncident') }}</p>
          <div class="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-2 lg:grid-cols-3">
            <UButton
              v-for="type in detail.incidentTypes" :key="type.id" icon="i-lucide-plus" color="neutral" variant="outline" block
              class="justify-start" :label="type.name" :disabled="busy" :data-add-incident="type.id" @click="openIncident(entry, type)"
            />
          </div>
        </div>
  
        <ul v-if="entry.incidents.length" class="mt-4 divide-y divide-default rounded-lg border border-default" data-incidents>
          <li v-for="incident in entry.incidents" :key="incident.id" class="flex items-start gap-3 px-3 py-2" :data-incident="incident.id">
            <span class="w-12 shrink-0 pt-0.5 text-sm tabular-nums text-muted">{{ formatTime(incident.createdAt) }}</span>
            <div class="min-w-0 flex-1">
              <p class="font-medium text-highlighted">{{ incident.type.name }}</p>
              <p class="text-xs text-muted">{{ $t('winch.driver', { name: incident.driver?.name ?? $t('winch.unknownUser') }) }}</p>
              <p v-if="incident.comment" class="mt-1 text-sm text-default">{{ incident.comment }}</p>
            </div>
            <ConfirmationDialog
              v-if="incident.deletable" variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
              :tooltip="$t('winch.deleteIncident')" :modal-title="$t('winch.deleteIncidentConfirm')"
              :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="() => removeIncident(incident)"
            />
          </li>
        </ul>
        <p v-else class="mt-4 text-sm text-muted">{{ $t('winch.noIncidents') }}</p>
  
        <!-- photos -->
        <div v-if="entry.photos.length || (detail.editable && entry.rope.active)" class="mt-4">
          <div class="mb-2 flex items-center justify-between gap-2">
            <h3 class="text-sm font-medium text-highlighted">{{ $t('winch.photos.title') }}</h3>
            <template v-if="detail.editable && entry.rope.active">
              <UButton
                icon="i-lucide-camera" color="neutral" variant="outline" size="sm" :label="$t('winch.photos.add')" :loading="uploading === entry.rope.id"
                :disabled="!!uploading" data-add-photo @click="pickPhoto(entry)"
              />
            </template>
          </div>
          <div v-if="entry.photos.length" class="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-3 lg:grid-cols-4">
            <MaintenanceWinchPhoto v-for="photo in entry.photos" :key="photo.id" :photo="photo" @delete="removePhoto" />
          </div>
          <p v-else class="text-sm text-muted">{{ $t('winch.photos.none') }}</p>
        </div>
  
        <!-- remark of the rope, at the bottom of every card so they line up -->
        <div class="mt-auto pt-4">
          <h3 class="text-sm font-medium text-highlighted">{{ $t('winch.ropeRemark') }}</h3>
          <template v-if="detail.remarksEditable">
            <UTextarea
              v-model="ropeRemarks[entry.rope.id]" :rows="2" autoresize class="mt-2 w-full" :placeholder="$t('winch.ropeRemarkPlaceholder')"
              :aria-label="$t('winch.ropeRemarkOf', { rope: entry.rope.name })" data-rope-remark-input
            />
            <div class="mt-2 flex flex-wrap items-center justify-between gap-2">
              <RemarkMeta :remark="entry.remark" />
              <UButton
                icon="i-lucide-save" :label="$t('common.save')" size="sm" class="ml-auto" :loading="busy"
                :disabled="(ropeRemarks[entry.rope.id] ?? '').trim() === (entry.remark.text ?? '')" data-save-rope-remark
                @click="saveRemark(`/maintenance/winch/days/${detail.date}/ropes/${entry.rope.id}/remark`, ropeRemarks[entry.rope.id] ?? '', entry.remark.version)"
              />
            </div>
          </template>
          <template v-else>
            <p v-if="entry.remark.text" class="mt-1 whitespace-pre-line text-sm text-default" data-rope-remark-text>{{ entry.remark.text }}</p>
            <p v-else class="mt-1 text-sm text-muted">{{ $t('winch.noRemark') }}</p>
            <RemarkMeta :remark="entry.remark" class="mt-1" />
          </template>
        </div>
      </section>
    </div>

    <!-- general remark of the day, below the ropes -->
    <section class="rounded-xl border border-default bg-default p-4" data-day-remark>
      <h2 class="font-semibold text-highlighted">{{ $t('winch.dayRemark') }}</h2>
      <template v-if="detail.remarksEditable">
        <UTextarea
          v-model="dayRemark" :rows="3" autoresize class="mt-3 w-full" :placeholder="$t('winch.dayRemarkPlaceholder')"
          :aria-label="$t('winch.dayRemark')" data-day-remark-input
        />
        <div class="mt-2 flex flex-wrap items-center justify-between gap-2">
          <RemarkMeta :remark="detail.remark" />
          <UButton
            icon="i-lucide-save" :label="$t('common.save')" :disabled="dayRemark.trim() === (detail.remark.text ?? '')" :loading="busy"
            class="ml-auto" data-save-day-remark @click="saveRemark(`/maintenance/winch/days/${detail.date}/remark`, dayRemark, detail.remark.version)"
          />
        </div>
      </template>
      <template v-else>
        <p v-if="detail.remark.text" class="mt-2 whitespace-pre-line text-sm text-default" data-day-remark-text>{{ detail.remark.text }}</p>
        <p v-else class="mt-2 text-sm text-muted">{{ $t('winch.noRemark') }}</p>
        <RemarkMeta :remark="detail.remark" class="mt-1" />
      </template>
    </section>

    <input ref="fileInput" type="file" accept="image/*" multiple class="hidden" data-photo-input @change="upload">

    <UModal v-model:open="incidentOpen" :title="incidentTitle">
      <template #body>
        <form class="space-y-4" data-incident-form @submit.prevent="addIncident">
          <p class="text-sm text-muted">{{ $t('winch.incidentHint') }}</p>
          <UFormField :label="$t('winch.comment')">
            <UTextarea v-model="incidentComment" :rows="2" autoresize class="w-full" :placeholder="$t('winch.commentPlaceholder')" data-incident-comment />
          </UFormField>
          <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <UButton :label="$t('common.cancel')" color="neutral" variant="outline" @click="incidentOpen = false" />
            <UButton type="submit" icon="i-lucide-check" :label="$t('winch.record')" :loading="busy" data-confirm-incident />
          </div>
        </form>
      </template>
    </UModal>
  </div>
</template>

<script lang="ts" setup>
import type { PropType } from 'vue'
import type { WinchDay, WinchIncident, WinchOptionRef, WinchPhoto, WinchRemark, WinchRopeEntries } from '~/types/winch'

/**
 * The page of a day in the winch book: incidents, photos and remarks per rope plus the remark of the day. What can be
 * changed comes from the flags of the response; the backend enforces the rules.
 */
const props = defineProps<{ detail: WinchDay }>()
const emit = defineEmits<{ update: [detail: WinchDay], reload: [] }>()

const { t, locale } = useI18n()
const toast = useToast()
const { formatDay, formatWhen } = useMaintenance()

const busy = ref(false)
const uploading = ref<string | null>(null)
const dayRemark = ref('')
const ropeRemarks = reactive<Record<string, string>>({})
const incidentOpen = ref(false)
const incidentRope = ref<WinchRopeEntries | null>(null)
const incidentType = ref<WinchOptionRef | null>(null)
const incidentComment = ref('')
const fileInput = useTemplateRef<HTMLInputElement>('fileInput')
const photoRope = ref<string | null>(null)

// the inputs follow the saved remarks, e.g. after somebody else changed them
watch(() => props.detail, (detail) => {
  dayRemark.value = detail.remark.text ?? ''
  detail.ropes.forEach((entry) => { ropeRemarks[entry.rope.id] = entry.remark.text ?? '' })
}, { immediate: true })

const formatTime = (timestamp: string) => new Date(timestamp).toLocaleTimeString(locale.value, { hour: '2-digit', minute: '2-digit' })

/** Incidents of a rope by kind, in the order they were recorded first. */
const countsOf = (entry: WinchRopeEntries) => {
  const counts = new Map<string, number>()
  entry.incidents.forEach(incident => counts.set(incident.type.name, (counts.get(incident.type.name) ?? 0) + 1))
  return [...counts].map(([name, count]) => ({ name, count }))
}

const incidentTitle = computed(() => incidentRope.value && incidentType.value
  ? t('winch.incidentTitle', { type: incidentType.value.name, rope: incidentRope.value.rope.name })
  : '')

const failed = (error: any) => {
  if (error?.statusCode === 409) {
    toast.add({ title: t('winch.conflict'), icon: 'i-lucide-refresh-cw', color: 'warning' })
    emit('reload')
    return
  }
  toast.add({ title: error?.statusCode === 413 ? t('winch.photos.tooLarge') : t('winch.saveError'), icon: 'i-lucide-bomb', color: 'error' })
}

const send = async (url: string, options: Record<string, unknown>) => {
  busy.value = true
  const { data, error } = await useAPI<WinchDay>(url, options)
  busy.value = false
  if (error.value) {
    failed(error.value)
    return false
  }
  emit('update', data.value as WinchDay)
  return true
}

const openIncident = (entry: WinchRopeEntries, type: WinchOptionRef) => {
  incidentRope.value = entry
  incidentType.value = type
  incidentComment.value = ''
  incidentOpen.value = true
}

const addIncident = async () => {
  if (!incidentRope.value || !incidentType.value) {
    return
  }
  const recorded = await send('/maintenance/winch/incidents', {
    method: 'POST',
    body: { ropeId: incidentRope.value.rope.id, typeId: incidentType.value.id, comment: incidentComment.value.trim() || null }
  })
  if (recorded) {
    incidentOpen.value = false
    toast.add({ title: t('winch.recorded', { type: incidentType.value.name }), icon: 'i-lucide-circle-check', color: 'primary' })
  }
}

const removeIncident = (incident: WinchIncident) => send(`/maintenance/winch/incidents/${incident.id}`, { method: 'DELETE' })
const removePhoto = (photo: WinchPhoto) => send(`/maintenance/winch/photos/${photo.id}`, { method: 'DELETE' })

// no toast: the line "last changed by" below the remark tells it was saved (toasts would cover the inputs on phones)
const saveRemark = (url: string, text: string, version: number | null) => send(url, { method: 'PUT', body: { text: text.trim() || null, version } })

const pickPhoto = (entry: WinchRopeEntries) => {
  photoRope.value = entry.rope.id
  fileInput.value?.click()
}

const upload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = [...(input.files ?? [])]
  input.value = ''
  const ropeId = photoRope.value
  if (!ropeId || !files.length) {
    return
  }
  uploading.value = ropeId
  for (const original of files) {
    const file = await scalePhoto(original)
    if (!isAcceptedPhoto(file)) {
      toast.add({ title: t('winch.photos.notAccepted', { name: original.name }), icon: 'i-lucide-bomb', color: 'error' })
      continue
    }
    const body = new FormData()
    body.append('file', file, file.name)
    const { data, error } = await useAPI<WinchDay>(`/maintenance/winch/ropes/${ropeId}/photos`, { method: 'POST', body })
    if (error.value) {
      failed(error.value)
      break
    }
    emit('update', data.value as WinchDay)
  }
  uploading.value = null
}

/** Who changed a remark last and when. */
const RemarkMeta = defineComponent({
  props: { remark: { type: Object as PropType<WinchRemark>, required: true } },
  setup(componentProps) {
    return () => componentProps.remark.text && componentProps.remark.updatedAt
      ? h('p', { class: 'text-xs text-muted' }, t('winch.remarkBy', {
          name: componentProps.remark.updatedBy?.name ?? t('winch.unknownUser'),
          when: formatWhen(componentProps.remark.updatedAt)
        }))
      : h('span')
  }
})
</script>
