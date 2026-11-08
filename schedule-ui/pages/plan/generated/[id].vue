<template>
  <AppPage :title="$t('plan.generatedDetailsInfo')" back="/plan/list" :back-label="$t('plan.backToList')" :error-message="errorMessage" :success-message="successMessage">
    <Spinner v-if="loading" />

    <div v-else class="space-y-4">
      <DataTable
        ref="table"
        v-model:pagination="pagination"
        :data="data"
        :columns="columns"
        loading-color="primary"
        loading-animation="carousel"
        :pagination-options="{ getPaginationRowModel: getPaginationRowModel() }"
      />

      <div class="flex justify-center">
        <UPagination
          :default-page="(table?.tableApi?.getState().pagination.pageIndex || 0) + 1"
          :sibling-count="1"
          :items-per-page="table?.tableApi?.getState().pagination.pageSize"
          :total="table?.tableApi?.getFilteredRowModel().rows.length"
          @update:page="(p) => { table?.tableApi?.setPageIndex(p - 1) }"
        />
      </div>
    </div>
  </AppPage>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import { getPaginationRowModel } from '@tanstack/vue-table'
import type { ParticipantAvailability } from '~/types/participantAvailability'
import type { Plan } from '~/types/plan'
import type { PlanEntry } from '~/types/planEntry'
const route = useRoute()
const { t, locale } = useI18n()

const userStore = useUserStore()
const { user } = storeToRefs(userStore)

const UButton = resolveComponent('UButton')
const UInput = resolveComponent('UInput')
const USelect = resolveComponent('USelect')

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const id = route.params.id
const loading = ref<boolean>(true)

const data = ref<PlanTableEntryRow[] | undefined>([])
const editingState = reactive<{ [key: string]: any }>({})
const availabilityData = ref<ParticipantAvailability[]>([])

const table = useTemplateRef('table')

type PlanTableEntryRow = {
  date: Date,
  service: {
    id: string,
    entryIds: string[],    // all plan entry ids associated with this date
    servicename: string,
    startTime: Date | undefined,
    endTime: Date | undefined,
    participants: {   // all participants associated with this service and date
      id: string,
      firstname: string,
      lastname: string,
      fullname: string
    }[]
  }[]
}

const columns = computed<TableColumn<PlanTableEntryRow>[]>(() => [
  {
    accessorKey: 'date',
    header: t('plan.date'),
    meta: { cardTitle: true, class: { td: 'font-medium text-highlighted whitespace-nowrap align-top' } },
    cell: ({ row }: { row: { getValue: (key: string) => any } }) => {
      return new Date(row.getValue('date')).toLocaleString(locale.value, {
        weekday: 'short',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    }
  },
  ...(data.value?.[0]?.service ?? []).map((service, index) => ({
    accessorKey: `service[${index}].servicename`,
    header: service.servicename,
    meta: { class: { td: 'align-top' } },
    cell: ({ row }: { row: { getValue: (key: string) => any; original: PlanTableEntryRow } }) => {
      const service = row.original.service[index]
      const cellIndex = service.id + "_" + row.getValue('date')
      const participants = service.participants
      const editButton = h(UButton, {
        color: 'neutral',
        variant: 'outline',
        icon: 'i-lucide-pencil',
        'aria-label': t('common.edit'),
        size: 'sm',
        onClick: () => startEdit()
      })
      const abortButton = h(UButton, {
        'aria-label': t('common.cancel'),
        color: 'neutral',
        variant: 'outline',
        icon: 'i-lucide-x',
        size: 'sm',
        onClick: () => editingState[cellIndex].editing = false
      })
      const saveButton = h(UButton, {
        'aria-label': t('common.save'),
        icon: 'i-lucide-save',
        size: 'sm',
        onClick: () => saveEntry()
      })
      const startTime = new Date(service.startTime ?? '').toLocaleString(locale.value, {
        hour: '2-digit',
        minute: '2-digit'
      })
      const endTime = new Date(service.endTime ?? '').toLocaleString(locale.value, {
        hour: '2-digit',
        minute: '2-digit'
      })
      const hours = service.startTime === undefined && service.endTime === undefined ? '' : startTime + ' - ' + endTime
      const startTimeInput = h(UInput, {
        modelValue: startTime,
        type: 'time',
        'onUpdate:modelValue': (value: string) => {
          editingState[cellIndex].startTime = value
        }
      })
      const endTimeInput = h(UInput, {
        modelValue: endTime,
        type: 'time',
        'onUpdate:modelValue': (value: string) => {
          editingState[cellIndex].endTime = value
        }
      })

      const allParticipants = Array.from(
        new Map(
          availabilityData.value?.filter(availability => availability.participant.participantServices?.some(s => s.service.id === service.id))
            .map(availability => [availability.participant.id, availability.participant])
        ).values()
      )

      const startEdit = async () => {
        editingState[cellIndex] = {}
        editingState[cellIndex].editing = true
        editingState[cellIndex].startTime = startTime
        editingState[cellIndex].endTime = endTime
        editingState[cellIndex].elements = []

        participants.forEach(participant => {
          const select = h(USelect, {
            defaultValue: participant.id,
            items: allParticipants,
            labelKey: 'fullname',
            valueKey: 'id',
            class: 'w-full',
            'onUpdate:modelValue': (selectedId: string) => {
              participantFields.value.selectedId = selectedId
            }
          })

          const participantFields = ref({
            select: select,
            selectedId: participant.id
          })
          editingState[cellIndex].elements.push(participantFields.value)
        })
      }

      const addParticipant = async () => {
        const select = h(USelect, {
            items: allParticipants,
            labelKey: 'fullname',
            valueKey: 'id',
            class: 'w-full',
            'onUpdate:modelValue': (selectedId: string) => {
              participantFields.value.selectedId = selectedId
            }
          })

          const participantFields = ref({
            select: select,
            selectedId: ''
          })
          editingState[cellIndex].elements.push(participantFields.value)
      }

      const saveEntry = async () => {
        const planEntry = ref<PlanEntry[]>([])
        await editingState[cellIndex].elements.forEach(async (element: { selectedId: string }) => {
          const offset = new Date(row.getValue('date')).getTimezoneOffset() / 60
          planEntry.value.push({
            id: '',
            startTime: new Date(new Date(row.getValue('date')).setHours(
              parseInt(editingState[cellIndex].startTime.split(':')[0]) - offset,
              parseInt(editingState[cellIndex].startTime.split(':')[1])
            )),
            endTime: new Date(new Date(row.getValue('date')).setHours(
              parseInt(editingState[cellIndex].endTime.split(':')[0]) - offset,
              parseInt(editingState[cellIndex].endTime.split(':')[1])
            )),
            date: row.original.date,
            participant: allParticipants.find(participant => participant.id === element.selectedId) || {
              id: '',
              firstname: '',
              lastname: '',
              fullname: '',
              email: '',
              dateOfBirth: new Date(),
              status: 'INACTIVE',
              maxAssignments: 0,
              registrationKey: '',
              participantServices: [],
              services: undefined
            },
            service: {
              id: service.id,
              servicename: service.servicename,
              addAtImport: false,
              settings: []
            }
          })
        })

        const { data: entryData, error: entryError } = await useAPI<PlanEntry[]>(`/plan/updatePlanEntry/` + id, { method: 'POST', body: JSON.stringify(planEntry.value) })

        if (entryError?.value?.message === 'error') {
          errorMessage.value = 'An error occurred while saving the data.'
          return
        }

        if(entryData.value && entryData.value.length > 0) {
          successMessage.value = t('plan.saveSuccess')
          row.original.service[index].startTime = entryData.value[0].startTime
          row.original.service[index].endTime = entryData.value[0].endTime
          row.original.service[index].entryIds = entryData.value.map(entry => entry.id)
          row.original.service[index].participants = entryData.value.map(entry => ({
            id: entry.participant.id,
            firstname: entry.participant.firstname,
            lastname: entry.participant.lastname,
            fullname: entry.participant.fullname
          }))
        }

        editingState[cellIndex].editing = false
      }

      const removeEntry = async () => {
        editingState[cellIndex].elements.pop()
      }

      if (!editingState[cellIndex]?.editing) {
        return h('div', { class: 'flex items-start justify-between gap-2' }, [
          h('div', { class: 'min-w-0' }, [
            hours ? h('p', { class: 'mb-1 text-xs text-muted' }, hours) : null,
            ...participants.map(participant => h('p', { class: 'text-sm' }, `${participant.lastname}, ${participant.firstname}`))
          ]),
          user.value?.roles.includes('ROLE_ADMIN') ? editButton : null
        ])
      }

      return h('div', { class: 'flex min-w-48 flex-col gap-2' }, [
        h('div', { class: 'flex justify-end gap-2' }, [abortButton, saveButton]),
        h('div', { class: 'grid grid-cols-2 gap-2' }, [startTimeInput, endTimeInput]),
        ...editingState[cellIndex].elements.map((element: any) => element.select),
        h('div', { class: 'flex justify-end gap-2' }, [
          h(UButton, {
            'aria-label': t('common.removeParticipant'),
            color: 'error',
            variant: 'ghost',
            icon: 'i-lucide-user-minus',
            size: 'sm',
            onClick: () => removeEntry()
          }),
          h(UButton, {
            color: 'neutral',
            variant: 'outline',
            icon: 'i-lucide-user-plus',
            'aria-label': t('common.addParticipant'),
            size: 'sm',
            onClick: () => addParticipant()
          })
        ])
      ])
    }
  })) || []
])

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  data.value = []

  try {
    // fetch the plan details and all services
    const { data: planData, error: planError } = await useAPI<Plan>(`/plan/fetch/` + id, { method: 'GET' })

    if (user.value?.roles.includes('ROLE_ADMIN')) {
      const { data: ad, error: availabilityError } = await useAPI<ParticipantAvailability[]>(`/plan/participantAvailability/fetch/` + id, { method: 'GET' })

      if (availabilityError?.value?.message === 'error') {
        errorMessage.value = 'An error occurred while fetching the data.'
        return
      }

      if (ad.value) {
        availabilityData.value = ad.value
      }
    }

    if (planError?.value?.message === 'error') {
      errorMessage.value = 'An error occurred while fetching the data.'
      return
    }

    if (planData.value?.entries) {
      // Extract unique dates
      const uniqueDates = Array.from(new Set(planData.value.entries.map(entry => new Date(entry.date).toISOString()))).sort((a, b) => new Date(a).getTime() - new Date(b).getTime())

      // Extract unique services
      const uniqueServices = Array.from(new Set(planData.value.entries.map(entry => entry.service.id))).map(serviceId => {
        const service = planData.value?.entries.find(entry => entry.service.id === serviceId)?.service
        return {
          id: service?.id || '',
          servicename: service?.servicename || '',
          startTime: undefined,
          endTime: undefined,
        }
      }).sort((a, b) => a.servicename.localeCompare(b.servicename))

      // Initialize data.value with dates and services
      data.value = uniqueDates.map(date => ({
        date: new Date(date),
        service: uniqueServices.map(service => ({
          ...service,
          entryIds: [],
          participants: []
        })) as PlanTableEntryRow['service']
      }))

      // Populate participants into the corresponding service
      planData.value.entries.forEach(entry => {
        const matchingRow = data.value?.find(row => row.date.getTime() === new Date(entry.date).getTime())
        if (matchingRow) {
          const matchingService = matchingRow.service.find(service => service.id === entry.service.id)
          if (matchingService) {
            matchingService.entryIds.push(entry.id)
            matchingService.startTime = entry.startTime
            matchingService.endTime = entry.endTime
            matchingService.participants.push({
              id: entry.participant.id,
              firstname: entry.participant.firstname,
              lastname: entry.participant.lastname,
              fullname: entry.participant.lastname + ', ' + entry.participant.firstname
            })
          }
        }
      })
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  } finally {
    loading.value = false
  }
}

const pagination = ref({
  pageIndex: 0,
  pageSize: 5
})

onMounted(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated']
})
</script>