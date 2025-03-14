<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-8/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('participants.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <UTable :data="listData" :columns="columns" class="flex-1" />
      <UPagination v-model:page="page" :total="totalItems" @update:page="fetchData"/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'

const { t, locale } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const page = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)
const listData = ref([])
const serviceItems = ref([])

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UInput = resolveComponent('UInput')
const UCheckbox = resolveComponent('UCheckbox')
const USelect = resolveComponent('USelect')

type Participant = {
  id: string
  version: number
  firstname: string
  lastname: string
  email: string
  dateOfBirth: string
  status: 'ACTIVE' | 'INACTIVE'
  services: string[]
}

const editingState = reactive<{ [key: string]: boolean }>({})
const editedRows = reactive<{ [key: string]: Participant }>({})

const columns = computed<TableColumn<Participant>[]>(() => [
  {
    accessorKey: 'lastname',
    header: t('participants.lastname'),
    cell: ({ row }) => {
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
    header: t('participants.firstname'),
    cell: ({ row }) => {
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
    header: t('participants.email'),
    cell: ({ row }) => {
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
    header: t('participants.dateOfBirth'),
    cell: ({ row }) => {
      const rowId = row.original.id
      const isEditing = ref(editingState[rowId] || false)

      return isEditing.value ?
        h(UInput, {
          modelValue: editedRows[rowId]?.dateOfBirth || new Date(row.original.dateOfBirth).toLocaleDateString('en-CA'),
          type: 'date',
          'onUpdate:modelValue': (value: string) => {
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
    header: t('participants.status'),
    cell: ({ row }) => {
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
          row.getValue('status') === 'ACTIVE' ? t('participants.active') : t('participants.passive')
        )
    }
  }, {
    accessorKey: 'services',
    header: t('participants.services'),
    cell: ({ row }) => {
      const rowId = row.original.id
      const isEditing = ref(editingState[rowId] || false)

      return isEditing.value ?
        h(USelect, {
          modelValue: editedRows[rowId]?.services || row.original.services.map((service: { id: string }) => service.id),
          'onUpdate:modelValue': (value: string[]) => {
            if (!editedRows[rowId]) {
              editedRows[rowId] = { ...row.original }
            }
            editedRows[rowId].services = value
          },
          multiple: true,
          valueKey: 'id',
          items: serviceItems.value,
          class: 'w-full'
        }) :
        row.getValue('services').map((service: { servicename: string }) => service.servicename).join(', ')
    }
  }, {
    accessorKey: 'actions',
    header: t('participants.actions'),
    cell: ({ row }) => {
      const rowId = row.original.id
      const isEditing = ref(editingState[rowId] || false)
      const editedRow = ref({ ...row.original })

      const startEdit = () => {
        editingState[rowId] = true
        if (!editedRows[rowId]) {
          editedRows[rowId] = {
            ...row.original,
            services: row.original.services.map((service: { id: string }) => service.id) // Convert to array of IDs
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
          // Replace service IDs with full service objects
          const updatedRow = {
            ...editedRows[rowId],
            services: editedRows[rowId].services.map((serviceId: string) =>
              serviceItems.value.find((service: { id: string }) => service.id === serviceId)
            )
          }

          const { error } = await useAPI(`/participant/save`, {
            method: 'POST',
            body: updatedRow
          })
          if (error.value) {
            errorMessage.value = t('participants.updateFailure')
          } else {
            successMessage.value = t('participants.updateSuccess')
            fetchData()
          }
        } catch (err) {          console.log(err)
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

      return isEditing.value ? [
        h(UButton, { onClick: saveEdit, variant:"subtle", icon:"i-lucide-save" }),
        h(UButton, { onClick: abortEdit, color:"error", variant:"subtle", icon:"i-lucide-x" })
      ] : [
        h(UButton, { onClick: startEdit, variant:"subtle", icon:"i-lucide-pen" }),
        h(UButton, { onClick: deleteRow, color:"error", variant:"subtle", icon:"i-lucide-trash" })
      ]
    }
  }
])

const fetchData = async () => {
  try {
    const { data, error } = await useAPI(`/participant/page?page=${page.value - 1}&size=${pageSize.value}`, { method: 'GET' })
    const { data: serviceData, error: serviceError } = await useAPI(`/service/list`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      listData.value = data.value.content
      totalItems.value = data.value.totalElements
    }
    serviceItems.value = serviceData.value.map((service: { id: string; servicename: string }) => ({
      id: service.id,
      label: service.servicename
    }))
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

watchEffect(() => {
  fetchData()
})


definePageMeta({
    middleware: ['authenticated']
})
</script>
