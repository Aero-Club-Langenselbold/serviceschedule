<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-12/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('participants.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <div class="flex px-4 py-3.5 border-b border-accented">
        <UInput v-model="filterValue" class="max-w-sm" :placeholder="$t('participants.filter')" @update:model-value="fetchDataDebounced" />
      </div>
      <UTable :data="listData?.content" :columns="columns" class="flex-1" />
      <UPagination v-model:page="page" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData"/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Page } from '~/types/page'
import type { Participant } from '~/types/participant'
import type { ParticipantService } from '~/types/participantService'
import type { Service } from '~/types/service'

const { t, locale } = useI18n()
const featureStore = useFeatureStore()
const { validateFeatures, features } = featureStore

await validateFeatures()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const page = ref(1)
const pageSize = ref(20)
const listData = ref<Page<Participant> | null>(null)
const serviceItems = ref<ParticipantService[] | null>([])

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UInput = resolveComponent('UInput')
const UCheckbox = resolveComponent('UCheckbox')

const editingState = reactive<{ [key: string]: boolean }>({})
const editedRows = reactive<{ [key: string]: Participant }>({})
let filterValue = ''

const toast = useToast()

const showToast = () => {
  toast.add({
    title: t('participants.registrationKeyCopied'),
    duration: 3000
  })
}

function debounce(func: (...args: any[]) => void, wait: number) {
  let timeout: ReturnType<typeof setTimeout>
  return (...args: any[]) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

const fetchDataDebounced = debounce(() => {
  fetchData()
}, 500)

const columns = computed<TableColumn<Participant>[]>(() => {
  const baseColumns = [
    {
      accessorKey: 'lastname',
      header: ({ column }: { column: TableColumn<Participant> }) => sortColumn(column, t('participants.lastname'), 'lastname'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h(UInput, {
            modelValue: editedRows[rowId]?.lastname || row.original.lastname,
            'onUpdate:modelValue': (value: string) => {
              if (!editedRows[rowId]) {
                editedRows[rowId] = { ...row.original }
              }
              editedRows[rowId].lastname = value
            }
          }) :
          row.getValue('lastname')
      }
    }, {
      accessorKey: 'firstname',
      header: ({ column }: { column: TableColumn<Participant> }) => sortColumn(column, t('participants.firstname'), 'firstname'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h(UInput, {
            modelValue: editedRows[rowId]?.firstname || row.original.firstname,
            'onUpdate:modelValue': (value: string) => {
              if (!editedRows[rowId]) {
                editedRows[rowId] = { ...row.original }
              }
              editedRows[rowId].firstname = value
            }
          }) :
          row.getValue('firstname')
      }
    }, {
      accessorKey: 'email',
      header: ({ column }: { column: TableColumn<Participant> }) => sortColumn(column, t('participants.email'), 'email'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h(UInput, {
            modelValue: editedRows[rowId]?.email || row.original.email,
            type: 'email',
            'onUpdate:modelValue': (value: string) => {
              if (!editedRows[rowId]) {
                editedRows[rowId] = { ...row.original }
              }
              editedRows[rowId].email = value
            }
          }) :
          row.getValue('email')
      }
    }, {
      accessorKey: 'dateOfBirth',
      header: ({ column }: { column: TableColumn<Participant> }) => sortColumn(column, t('participants.dateOfBirth'), 'dateOfBirth'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h(UInput, {
            modelValue: editedRows[rowId]?.dateOfBirth || new Date(row.original.dateOfBirth ?? '').toLocaleDateString('en-CA'),
            type: 'date',
            'onUpdate:modelValue': (value: Date) => {
              if (!editedRows[rowId]) {
                editedRows[rowId] = { ...row.original }
              }
              editedRows[rowId].dateOfBirth = value
            }
          }) :
          new Date(row.getValue('dateOfBirth')).toLocaleString(locale.value, {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
          })
      }
    }, {
      accessorKey: 'status',
      header: ({ column }: { column: TableColumn<Participant> }) => sortColumn(column, t('participants.status'), 'status'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const color = {
          ACTIVE: 'success' as const,
          INACTIVE: 'error' as const
        }[row.getValue('status') as string]

        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h(UCheckbox, {
            modelValue: editedRows[rowId]?.status === 'ACTIVE' || (!editedRows[rowId] && row.original.status === 'ACTIVE'),
            'onUpdate:modelValue': (value: boolean) => {
              if (!editedRows[rowId]) {
                editedRows[rowId] = { ...row.original }
              }
              editedRows[rowId].status = value ? 'ACTIVE' : 'INACTIVE'
            }
          }) :
          h(UBadge, { class: 'capitalize', variant: 'subtle', color }, () =>
            row.getValue('status') === 'ACTIVE' ? t('participants.active') : t('participants.inactive')
          )
      }
    }, {
      accessorKey: 'minAssignments',
      header: ({ column }: { column: TableColumn<Participant> }) => sortColumn(column, t('participants.minAssignments'), 'minAssignments'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h(UInput, {
            modelValue: editedRows[rowId]?.minAssignments || row.original.minAssignments,
            type: 'number',
            'onUpdate:modelValue': (value: number) => {
              if (!editedRows[rowId]) {
                editedRows[rowId] = { ...row.original }
              }
              editedRows[rowId].minAssignments = value
            }
          }) :
          row.getValue('minAssignments')
      }
    }, {
      accessorKey: 'maxAssignments',
      header: ({ column }: { column: TableColumn<Participant> }) => sortColumn(column, t('participants.maxAssignments'), 'maxAssignments'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h(UInput, {
            modelValue: editedRows[rowId]?.maxAssignments || row.original.maxAssignments,
            type: 'number',
            'onUpdate:modelValue': (value: number) => {
              if (!editedRows[rowId]) {
                editedRows[rowId] = { ...row.original }
              }
              editedRows[rowId].maxAssignments = value
            }
          }) :
          row.getValue('maxAssignments')
      }
    }, {
      accessorKey: 'services',
      header: t('participants.services'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)

        return isEditing.value ?
          h('div', {}, [
            h('div', { class: 'flex items-center mb-2 w-full' }, [
              h('span', { class: 'text-sm font-bold mr-auto' }, t('participants.services')),
              h('span', { class: 'text-sm font-bold ml-auto' }, t('participants.autoAssign'))
            ]),
            h('div', {}, serviceItems.value?.map((item) =>
              h('div', { key: item.service.id, class: 'flex items-center mb-2 w-full' }, [
                h(UCheckbox, {
                  modelValue: editedRows[rowId]?.participantServices?.some((ps) => ps.service.id === item.service.id) || row.original.participantServices?.some((ps) => ps.service.id === item.service.id),
                  onChange: () => {
                    if (!editedRows[rowId]) {
                      editedRows[rowId] = { ...row.original }
                    }
                    const serviceExists = editedRows[rowId].participantServices?.some((ps: ParticipantService) => ps.service.id === item.service.id)
                    if (serviceExists) {
                      editedRows[rowId].participantServices = editedRows[rowId].participantServices?.filter((ps: ParticipantService) => ps.service.id !== item.service.id)
                    } else {
                      editedRows[rowId].participantServices = [...(editedRows[rowId].participantServices || []), item]
                    }
                  },
                  class: 'mr-2'
                }),
                h('span', { class: 'mr-4' }, item.service.servicename),
                h(UCheckbox, {
                  modelValue: editedRows[rowId]?.participantServices?.find((ps) => ps.service.id === item.service.id)?.autoAssign || row.original.participantServices?.find((ps) => ps.service.id === item.service.id)?.autoAssign,
                  'onUpdate:modelValue': (value: boolean) => {
                    if (!editedRows[rowId]) {
                      editedRows[rowId] = { ...row.original }
                    }
                    const service = editedRows[rowId].participantServices?.find((ps) => ps.service.id === item.service.id)
                    if (service) {
                      service.autoAssign = value
                    }
                  },
                  class: 'ml-auto'
                })
              ])
            ))
          ]):
          row.original.participantServices?.map((ps: ParticipantService) => ps.service.servicename).join(', ')
      }
    }
  ]

  // push registration key only if the proper feature is disabled
  if (!features || !features.some(feature => feature.id === 'vfLoginFeature')) {
    baseColumns.push({
      accessorKey: 'registrationKey',
      header: t('participants.registrationKey'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        return h('div', { class: 'flex items-center justify-between' }, [
          h('span', {}, row.getValue('registrationKey')),
          h(UButton, {
            color: 'neutral',
            variant: 'subtle',
            icon: 'i-lucide-copy',
            onClick: () => {
              navigator.clipboard.writeText(row.getValue('registrationKey'))
              .then(() => {
                showToast()
              })
            }
          })
        ])
      }
    })
  }

  // push actions
  baseColumns.push({
      accessorKey: 'actions',
      header: t('participants.actions'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const rowId = row.original.id
        const isEditing = ref(editingState[rowId] || false)
        const editedRow = ref({ ...row.original })

        const startEdit = () => {
          editingState[rowId] = true
          if (!editedRows[rowId]) {
            editedRows[rowId] = {
                ...row.original,
                services: undefined
            }
          }
        }

        const abortEdit = () => {
          editingState[rowId] = false
          isEditing.value = false
          editedRow.value = { ...row.original }
        }

        const saveEdit = async () => {
          try {
            const { error } = await useAPI(`/participant/save`, {
              method: 'POST',
              body: editedRows[rowId]
            })
            if (error.value) {
              errorMessage.value = t('participants.updateFailure')
            } else {
              successMessage.value = t('participants.updateSuccess')
              fetchData()
            }
          } catch (err) {
            errorMessage.value = 'An error occurred while updating the data.'
          } finally {
            editingState[rowId] = false
            delete editedRows[rowId]
          }
        }

        const deleteRow = async () => {
          try {
            const { error } = await useAPI(`/participant/delete/${row.original.id}`, {
              method: 'DELETE'
            })
            if (error.value) {
              errorMessage.value = t('participants.deleteFailure')
            } else {
              successMessage.value = t('participants.deleteSuccess')
              fetchData()
            }
          } catch (err) {
            errorMessage.value = 'An error occurred while deleting the data.'
          }
        }

        return isEditing.value ?
          h('div', {}, [
            h(UButton, { onClick: saveEdit, variant:"subtle", icon:"i-lucide-save", class: "mr-2" }),
            h(UButton, { onClick: abortEdit, color:"error", variant:"subtle", icon:"i-lucide-x" })
          ]) :
          h('div', {}, [
            h(UButton, { onClick: startEdit, variant:"subtle", icon:"i-lucide-pen", class: "mr-2" }),
            h(UButton, { onClick: deleteRow, color:"error", variant:"subtle", icon:"i-lucide-trash" })
          ])
      }
    })

  return baseColumns
})

let sort = 'lastname'
let direction = 'asc'
const sortColumn = (column: any, label: string, accessor: string) => {
  const isSorted = column.getIsSorted()

  return h(UButton, {
    color: 'neutral',
    variant: 'ghost',
    label: label,
    icon: isSorted
      ? isSorted === 'asc'
        ? 'i-lucide-arrow-up-narrow-wide'
        : 'i-lucide-arrow-down-wide-narrow'
      : 'i-lucide-arrow-up-down',
    class: '-mx-2.5',
    onClick: () => {
      column.toggleSorting(column.getIsSorted() === 'asc')
      sort = accessor
      direction = isSorted === 'asc' ? 'desc' : 'asc'
      fetchData()
    }
  })
}

const fetchData = async () => {
  try {
    const { data: participantData, error: participantError } = await useAPI<Page<Participant>>(`/participant/page?page=${page.value - 1}&size=${pageSize.value}&sort=${sort},${direction}&search=${filterValue}`, { method: 'GET' })
    const { data: serviceData, error: serviceError } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })
    if (participantError.value && participantError.value?.statusCode !== 200) {
      errorMessage.value = participantError.value.data.message
    } else {
      listData.value = participantData.value
    }
    serviceItems.value = (serviceData.value ?? []).map(service => ({
      participant: null,
      service: service,
      autoAssign: true
    }))
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

watchEffect(() => {
  fetchData()
})


definePageMeta({
    middleware: ['authenticated', 'schedule-admin']
})
</script>
