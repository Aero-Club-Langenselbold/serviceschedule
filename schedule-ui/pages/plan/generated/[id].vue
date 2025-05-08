<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-6 mb-6">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('plan.generatedDetailsInfo') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />

      <div class="flex justify-start mb-4 mt-4">
        <UButton color="neutral" variant="subtle" icon="i-lucide-arrow-left" @click="goBack">
          {{ $t('plan.backToList') }}
        </UButton>
      </div>

      <Spinner v-if="loading" />

      <div v-else class="w-full space-y-4 pb-4">
        <UTable
          ref="table"
          v-model:pagination="pagination"
          :data="data"
          :columns="columns"
          loading-color="primary"
          loading-animation="carousel"
          :pagination-options="{ getPaginationRowModel: getPaginationRowModel() }"
          class="flex-1 max-h-[600px]"
          sticky />

        <div class="flex justify-center border-t border-(--ui-border) pt-4">
            <UPagination
              :default-page="(table?.tableApi?.getState().pagination.pageIndex || 0) + 1"
              :items-per-page="table?.tableApi?.getState().pagination.pageSize"
              :total="table?.tableApi?.getFilteredRowModel().rows.length"
              @update:page="(p) => { table?.tableApi?.setPageIndex(p - 1) }"
            />
        </div>
      </div>
    </div>
  </div>
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

const goBack = () => {
  data.value = []
  navigateTo('/plan/list')
}

const columns = computed<TableColumn<PlanTableEntryRow>[]>(() => [
  {
    accessorKey: 'date',
    header: t('plan.date'),
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
    cell: ({ row }: { row: { getValue: (key: string) => any; original: PlanTableEntryRow } }) => {
      const service = row.original.service[index]
      const cellIndex = service.id + "_" + row.getValue('date')
      const participants = service.participants
      const editButton = h(UButton, {
        variant: 'subtle',
        icon: 'i-lucide-edit',
        size: 'sm',
        class: 'mr-2',
        onClick: () => startEdit()
      })
      const abortButton = h(UButton, {
        color: 'error',
        variant: 'subtle',
        icon: 'i-lucide-x',
        size: 'sm',
        class: 'mr-2',
        onClick: () => editingState[cellIndex].editing = false
      })
      const saveButton = h(UButton, {
        variant: 'subtle',
        icon: 'i-lucide-save',
        size: 'sm',
        class: 'mr-2',
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
            class: 'w-40',
            'onUpdate:modelValue': (selectedId: string) => {
              participantFields.value.selectedId = selectedId
            }
          })

          const participantFields = ref({
            lineFeed: h('br'),
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
            class: 'w-40',
            'onUpdate:modelValue': (selectedId: string) => {
              participantFields.value.selectedId = selectedId
            }
          })

          const participantFields = ref({
            lineFeed: h('br'),
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

      const elements: any[] = []
      if (!editingState[cellIndex]?.editing) {
        if (user.value?.roles.includes('ROLE_ADMIN')) {
          elements.push(editButton)
          elements.push(h('br'))
        }
        elements.push(h('label', {}, hours))
        elements.push(h('br'))
        participants.forEach(participant => {
          elements.push(h('label', {}, `${participant.lastname}, ${participant.firstname}`))
          elements.push(h('br'))
        })
      } else {
        elements.push(abortButton)
        elements.push(saveButton)
        elements.push(h('br'))
        elements.push(startTimeInput)
        elements.push(" - ")
        elements.push(endTimeInput)

        editingState[cellIndex].elements.forEach((element: any) => {
          elements.push(element.lineFeed)
          elements.push(element.select)
          elements.push(element.removeSelect)
        })

        elements.push(h(UButton, {
          color: 'error',
          variant: 'subtle',
          icon: 'i-lucide-x',
          size: 'sm',
          class: 'ml-2',
          onClick: () => removeEntry()
        }))
        elements.push(h('br'))
        elements.push(h(UButton, {
          variant: 'subtle',
          icon: 'i-lucide-plus',
          size: 'sm',
          class: 'mr-2',
          onClick: () => addParticipant()
        }))
      }
      return elements
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