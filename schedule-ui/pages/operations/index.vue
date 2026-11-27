<template>
  <AppPage :title="$t('operations.overview.title')" :description="$t('operations.overview.description')" :error-message="errorMessage">
    <template v-if="canManageOperations" #actions>
      <UButton to="/operations/archive" icon="i-lucide-archive" color="neutral" variant="outline" :label="$t('operations.archive.title')" />
    </template>

    <p v-if="loaded && !overview?.aircraft.length" class="text-sm text-muted">{{ $t('operations.overview.noAircraft') }}</p>
    <div class="grid gap-4 md:grid-cols-2">
      <section v-for="aircraft in overview?.aircraft ?? []" :key="aircraft.aircraftId" class="rounded-xl border border-default bg-default p-4" :data-aircraft="aircraft.aircraftId">
        <h2 class="text-lg font-semibold text-highlighted">{{ aircraft.registration }}</h2>
        <p class="text-sm text-muted">{{ aircraft.aircraftType }}</p>

        <div class="mt-4 divide-y divide-default">
          <div v-for="kind in CHECKLIST_KINDS" :key="kind" class="flex flex-col gap-3 py-3 first:pt-0 last:pb-0" :data-kind="kind">
            <div class="flex items-center justify-between gap-2">
              <p class="font-medium text-highlighted">{{ kindLabel(kind) }}</p>
              <template v-if="template(aircraft, kind)">
                <UBadge v-if="today(aircraft, kind)" :color="statusColor(today(aircraft, kind)!.status)" variant="subtle" :label="statusLabel(today(aircraft, kind)!.status)" />
                <UBadge v-else color="neutral" variant="outline" :label="$t('operations.overview.notStarted')" />
              </template>
            </div>

            <template v-if="template(aircraft, kind)">
              <p v-if="today(aircraft, kind)" class="text-sm text-muted">
                {{ $t('operations.checklist.progress', { checked: today(aircraft, kind)!.checkedCount, total: today(aircraft, kind)!.itemCount }) }}
                · {{ $t('operations.checklist.createdBy') }}: {{ today(aircraft, kind)!.createdBy?.name ?? $t('operations.checklist.unknownUser') }}
              </p>
              <p v-else class="text-sm text-muted">{{ $t('operations.overview.items', template(aircraft, kind)!.items.length) }}</p>
              <div class="flex flex-wrap items-center gap-2">
                <UButton :to="todayPath(aircraft.aircraftId, kind)" icon="i-lucide-clipboard-check" :label="$t('operations.overview.open')" data-open />
                <template v-if="canManageChecklists">
                  <UButton
                    color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="$t('operations.template.editShort')"
                    data-edit-template @click="edit(aircraft, kind)"
                  />
                  <UButton
                    :to="`/operations/print/${aircraft.aircraftId}/${kind.toLowerCase()}`" target="_blank" color="neutral" variant="ghost"
                    icon="i-lucide-qr-code" :aria-label="$t('operations.print.open')" data-print
                  />
                  <ConfirmationDialog
                    variant="ghost" color="error" icon="i-lucide-trash-2" :loading="false" :button-label="undefined" button-class=""
                    :tooltip="$t('operations.template.delete')" :modal-title="$t('operations.template.deleteConfirm')"
                    :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')" :on-confirm="() => remove(aircraft, kind)"
                  />
                </template>
              </div>
            </template>
            <div v-else class="flex flex-wrap items-center justify-between gap-2">
              <p class="text-sm text-muted">{{ $t('operations.overview.noTemplate') }}</p>
              <UButton
                v-if="canManageChecklists" color="neutral" variant="outline" icon="i-lucide-plus" :label="$t('operations.template.create')"
                data-create-template @click="edit(aircraft, kind)"
              />
            </div>
          </div>
        </div>
      </section>
    </div>

    <OperationsChecklistTemplateForm
      v-if="editing" v-model:open="formOpen" :aircraft-id="editing.aircraft.aircraftId" :registration="editing.aircraft.registration"
      :kind="editing.kind" :template="template(editing.aircraft, editing.kind)" @saved="load"
    />
  </AppPage>
</template>

<script lang="ts" setup>
import type { AircraftChecklists, ChecklistKind, OperationsOverview } from '~/types/operations'

const { t } = useI18n()
const toast = useToast()
const { kindLabel, statusLabel, statusColor } = useOperations()
const { canManageChecklists, canManageOperations } = storeToRefs(useUserStore())

const overview = ref<OperationsOverview | null>(null)
const loaded = ref(false)
const errorMessage = ref<string | null>(null)
const editing = ref<{ aircraft: AircraftChecklists, kind: ChecklistKind } | null>(null)
const formOpen = ref(false)

const template = (aircraft: AircraftChecklists, kind: ChecklistKind) => kind === 'START' ? aircraft.startTemplate : aircraft.endTemplate
const today = (aircraft: AircraftChecklists, kind: ChecklistKind) => kind === 'START' ? aircraft.todayStart : aircraft.todayEnd

const load = async () => {
  const { data, error } = await useAPI<OperationsOverview>('/operations/overview', { method: 'GET' })
  loaded.value = true
  if (error.value) {
    errorMessage.value = t('operations.loadError')
    return
  }
  errorMessage.value = null
  overview.value = data.value as OperationsOverview
}

const edit = (aircraft: AircraftChecklists, kind: ChecklistKind) => {
  editing.value = { aircraft, kind }
  formOpen.value = true
}

const remove = async (aircraft: AircraftChecklists, kind: ChecklistKind) => {
  const { error } = await useAPI(`/operations/aircraft/${aircraft.aircraftId}/templates/${kind}`, { method: 'DELETE' })
  if (error.value) {
    toast.add({ title: t('operations.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  await load()
}

onMounted(load)

definePageMeta({
  middleware: ['authenticated']
})
</script>
