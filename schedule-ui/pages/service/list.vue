<template>
  <AppPage :title="$t('service.list')" :error-message="errorMessage" :success-message="successMessage">
    <template #actions>
      <UButton to="/service/add" icon="i-lucide-plus" :label="$t('nav.service.add')" />
    </template>
    <DataTable v-model:expanded="expanded" :data="listData" :columns="columns">
      <template #expanded="{ row }">
        <div class="space-y-3 md:p-2">
          <ServiceSettings
            :settings="row.original.settings"
            :editable="editingState[row.original.id]"
            @remove="deleteSetting(row.original.id, $event)"
          />
          <UButton v-if="editingState[row.original.id]" color="neutral" variant="outline" icon="i-lucide-plus" @click="addSetting(row.original.id)">
            {{ $t('service.addSetting') }}
          </UButton>
        </div>
      </template>
    </DataTable>
  </AppPage>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Service } from '~/types/service'

const { t } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const UInput = resolveComponent('UInput')
const UCheckbox = resolveComponent('UCheckbox')

const listData = ref<Service[] | undefined>([])
const editingState = reactive<{ [key: string]: boolean }>({})
const editedRows = reactive<{ [key: string]: Service }>({})

const columns = computed<TableColumn<Service>[]>(() => [
  {
    id: 'expand',
    meta: { class: { th: 'w-12', td: 'max-md:hidden' } },
    cell: ({ row }) =>
      h(UButton, {
        color: 'neutral',
        variant: 'ghost',
        icon: 'i-lucide-chevron-down',
        'aria-label': t('common.toggleDetails'),
        square: true,
        ui: {
          leadingIcon: [
            'transition-transform',
            row.getIsExpanded() ? 'duration-200 rotate-180' : ''
          ]
        },
        onClick: () => row.toggleExpanded()
      })
  }, {
    accessorKey: 'servicename',
    header: t('service.name'),
    meta: { cardTitle: true, class: { td: 'font-medium text-highlighted' } },
    cell: ({ row }) => {
      const rowId = row.original.id
      const isEditing = ref(editingState[rowId] || false)

      return isEditing.value ?
        h(UInput, {
          modelValue: editedRows[rowId]?.servicename || row.original.servicename,
          'onUpdate:modelValue': (value: string) => {
            if (!editedRows[rowId]) {
              editedRows[rowId] = { ...row.original }
            }
            editedRows[rowId].servicename = value
          }
        }) :
        row.getValue('servicename')
    }
  }, {
    accessorKey: 'addAtImport',
    header: t('service.addAtImport'),
    cell: ({ row }) => {
      const rowId = row.original.id
      const isEditing = ref(editingState[rowId] || false)

      return h(UCheckbox, {
        modelValue: editedRows[rowId]?.addAtImport || row.original.addAtImport,
        'onUpdate:modelValue': (value: boolean) => {
          if (!editedRows[rowId]) {
            editedRows[rowId] = { ...row.original }
          }
          editedRows[rowId].addAtImport = value
        },
        disabled: !isEditing.value,
      })
    }
  }, {
    accessorKey: 'actions',
    header: '',
    cell: ({ row }) => {
      const rowId = row.original.id
      const isEditing = ref(editingState[rowId] || false)
      const editedRow = ref({ ...row.original })

      const startEdit = () => {
        editingState[rowId] = true
        if (!editedRows[rowId]) {
          editedRows[rowId] = {
            ...row.original
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
          const updatedRow = {
            ...editedRows[rowId]
          }

          const { error } = await useAPI(`/service/save`, {
            method: 'POST',
            body: updatedRow
          })
          if (error.value) {
            errorMessage.value = t('service.updateFailure')
          } else {
            await fetchData()
            successMessage.value = t('service.updateSuccess')
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
          const { error } = await useAPI(`/service/delete/${row.original.id}`, {
            method: 'DELETE'
          })
          if (error.value) {
            errorMessage.value = t('service.deleteFailure')
          } else {
            await fetchData()
            successMessage.value = t('service.deleteSuccess')
          }
        } catch (err) {
          errorMessage.value = 'An error occurred while deleting the data.'
        }
      }

      return h('div', { class: 'flex justify-end gap-2' }, [
        h(UButton, {
          'aria-label': t('common.toggleDetails'),
          color: 'neutral',
          variant: 'ghost',
          icon: row.getIsExpanded() ? 'i-lucide-chevron-up' : 'i-lucide-chevron-down',
          class: 'md:hidden mr-auto',
          onClick: () => row.toggleExpanded()
        }),
        ...(isEditing.value ? [
          h(UButton, { 'aria-label': t('common.save'), onClick: saveEdit, icon:"i-lucide-save" }),
          h(UButton, { 'aria-label': t('common.cancel'), onClick: abortEdit, color:"neutral", variant:"outline", icon:"i-lucide-x" })
        ] : [
          h(UButton, { 'aria-label': t('common.edit'), onClick: startEdit, color:"neutral", variant:"outline", icon:"i-lucide-pencil" }),
          h(UButton, { 'aria-label': t('common.delete'), onClick: deleteRow, color:"error", variant:"ghost", icon:"i-lucide-trash" })
        ])
      ])
    }
  }
])

const expanded = ref({})

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data, error } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else if(data.value) {
      listData.value = data.value
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const addSetting = (rowId: string) => {
  if (!editedRows[rowId]) {
    const foundRow = listData.value?.find(row => row.id === rowId)
    if (foundRow) {
      editedRows[rowId] = { ...foundRow }
    } else {
      editedRows[rowId] = { id: rowId, servicename: '', addAtImport: false, settings: [] }
    }
  }
  if (!editedRows[rowId].settings) {
    editedRows[rowId].settings = []
  }
  editedRows[rowId].settings.push({
    rangeFrom: '',
    rangeTo: '',
    mandatoryAssignees: 0,
    saturdays: false,
    sundays: false,
    publicHolidays: false,
    startTime: '',
    endTime: ''
  })
}

const deleteSetting = (rowId: string, index: number) => {
  if (editedRows[rowId] && editedRows[rowId].settings) {
    editedRows[rowId].settings.splice(index, 1)
  }
}

watchEffect(() => {
  fetchData()
})


definePageMeta({
  middleware: ['authenticated', 'schedule-admin']
})
</script>