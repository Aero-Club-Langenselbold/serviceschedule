<template>
  <UModal v-model:open="open" :title="defect ? $t('maintenance.defect.edit') : $t('maintenance.defect.add')">
    <template #body>
      <form id="defect-form" class="space-y-5" @submit.prevent="save">
        <!-- the defect book reports for any aircraft or equipment, the board page for its own -->
        <UFormField v-if="!defect && !aircraftId" :label="$t('maintenance.defect.aircraft')" required>
          <USelect v-model="chosenAircraft" :items="aircraftItems" :placeholder="$t('maintenance.defect.aircraftPlaceholder')" class="w-full" data-aircraft-select />
        </UFormField>
        <UFormField :label="$t('maintenance.defect.title')" required>
          <UInput v-model="form.title" :placeholder="$t('maintenance.defect.titlePlaceholder')" class="w-full" autofocus />
        </UFormField>
        <UFormField :label="$t('maintenance.priority.name')">
          <USelect v-model="form.priority" :items="priorityItems" class="w-full" />
        </UFormField>
        <UFormField :label="$t('maintenance.defect.description')">
          <UTextarea v-model="description" :rows="3" autoresize class="w-full" :placeholder="$t('maintenance.defect.descriptionPlaceholder')" />
        </UFormField>
        <UFormField v-if="canManageMaintenance" :label="$t('maintenance.defect.assignees')" :help="$t('maintenance.defect.assigneesHint')">
          <USelectMenu
            v-model="assigneeIds" :items="userItems" value-key="value" multiple class="w-full"
            :placeholder="$t('maintenance.defect.assigneesPlaceholder')" data-assignee-select
          />
        </UFormField>
      </form>
    </template>
    <template #footer>
      <div class="flex w-full flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <UButton color="neutral" variant="outline" :label="$t('common.cancel')" @click="open = false" />
        <UButton type="submit" form="defect-form" :label="$t('common.save')" :loading="saving" :disabled="!canSave" />
      </div>
    </template>
  </UModal>
</template>

<script lang="ts" setup>
import type { Aircraft, Defect, DefectDetail, DefectInput, Person } from '~/types/maintenance'

/** Reports a defect (for aircraftId, or for one chosen from aircraft) or edits one; maintenance administrators also assign it. */
const props = defineProps<{ aircraftId?: string | null, aircraft?: Aircraft[], defect?: Defect | null }>()
const emit = defineEmits<{ saved: [detail: DefectDetail] }>()
const open = defineModel<boolean>('open', { default: false })

const { t } = useI18n()
const toast = useToast()
const { priorityItems } = useMaintenance()
const { canManageMaintenance } = storeToRefs(useUserStore())

const form = ref<Omit<DefectInput, 'assigneeIds'>>({ title: '', description: null, priority: 'NORMAL' })
const description = ref('')
const chosenAircraft = ref<string | undefined>(undefined)
const assigneeIds = ref<string[]>([])
const users = ref<Person[]>([])
const saving = ref(false)

const aircraftItems = computed(() => (props.aircraft ?? []).map(item => ({ label: `${item.registration} · ${item.type}`, value: item.id })))
const userItems = computed(() => users.value.map(user => ({ label: user.name, value: user.id })))
const targetAircraft = computed(() => props.aircraftId ?? chosenAircraft.value)
const canSave = computed(() => !!form.value.title.trim() && (!!props.defect || !!targetAircraft.value))

const loadUsers = async () => {
  if (!canManageMaintenance.value || users.value.length) {
    return
  }
  const { data } = await useAPI<Person[]>('/maintenance/defects/assignable-users', { method: 'GET' })
  users.value = data.value ?? []
}

watch(open, (isOpen) => {
  if (isOpen) {
    form.value = { title: props.defect?.title ?? '', description: null, priority: props.defect?.priority ?? 'NORMAL' }
    description.value = props.defect?.description ?? ''
    chosenAircraft.value = props.aircraft?.length === 1 ? props.aircraft[0]!.id : undefined
    assigneeIds.value = props.defect?.assignees.map(person => person.id) ?? []
    loadUsers()
  }
}, { immediate: true })

const save = async () => {
  if (!canSave.value) {
    return
  }
  saving.value = true
  // only maintenance administrators send assignees, for everybody else null keeps them
  const body: DefectInput = { ...form.value, description: description.value, assigneeIds: canManageMaintenance.value ? assigneeIds.value : null }
  const { data, error } = props.defect
    ? await useAPI<DefectDetail>(`/maintenance/defects/${props.defect.id}`, { method: 'PUT', body })
    : await useAPI<DefectDetail>(`/maintenance/aircraft/${targetAircraft.value}/defects`, { method: 'POST', body })
  saving.value = false

  if (error.value) {
    toast.add({ title: t('maintenance.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  open.value = false
  emit('saved', data.value as DefectDetail)
}
</script>
