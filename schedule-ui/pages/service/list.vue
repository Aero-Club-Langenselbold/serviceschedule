<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-9/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('service.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <UTable v-model:expanded="expanded" :data="listData" :columns="columns" :ui="{ tr: 'data-[expanded=true]:bg-(--ui-bg-elevated)/50' }" class="flex-1">
        <template #expanded="{ row }">
          <table class="min-w-full border border-gray-200 mt-4">
            <thead>
              <tr>
                <th class="px-4 py-2 border">{{ $t('service.rangeFrom') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.rangeTo') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.mandatoryAssignees') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.saturdays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.sundays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.publicHolidays') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.startTime') }}</th>
                <th class="px-4 py-2 border">{{ $t('service.endTime') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(setting, index) in row.original.settings" :key="index">
                <td class="border px-4 py-2">
                  <template v-if="editingState[row.original.id]">
                    <USelect v-model="row.original.settings[index].rangeFrom" :items="dateRangeItems" class="w-48" />
                  </template>
                  <template v-else>
                    {{ getLabelForValue(setting.rangeFrom) }}
                  </template>
                </td>
                <td class="border px-4 py-2">
                  <template v-if="editingState[row.original.id]">
                    <USelect v-model="row.original.settings[index].rangeTo" :items="dateRangeItems" class="w-48" />
                  </template>
                  <template v-else>
                    {{ getLabelForValue(setting.rangeTo) }}
                  </template>
                </td>
                <td class="border px-4 py-2">
                  <template v-if="editingState[row.original.id]">
                    <UInput type="number" v-model="row.original.settings[index].mandatoryAssignees" />
                  </template>
                  <template v-else>
                    {{ setting.mandatoryAssignees }}
                  </template>
                </td>
                <td class="border px-4 py-2 text-center">
                  <template v-if="editingState[row.original.id]">
                    <UCheckbox v-model="row.original.settings[index].saturdays" />
                  </template>
                  <template v-else>
                    <UBadge :color="setting.saturdays ? 'success' : 'error'">{{ setting.saturdays ? $t('general.yes') : $t('general.no') }}</UBadge>
                  </template>
                </td>
                <td class="border px-4 py-2 text-center">
                  <template v-if="editingState[row.original.id]">
                    <UCheckbox v-model="row.original.settings[index].sundays" />
                  </template>
                  <template v-else>
                    <UBadge :color="setting.sundays ? 'success' : 'error'">{{ setting.sundays ? $t('general.yes') : $t('general.no') }}</UBadge>
                  </template>
                </td>
                <td class="border px-4 py-2 text-center">
                  <template v-if="editingState[row.original.id]">
                    <UCheckbox v-model="row.original.settings[index].publicHolidays" />
                  </template>
                  <template v-else>
                    <UBadge :color="setting.publicHolidays ? 'success' : 'error'">{{ setting.publicHolidays ? $t('general.yes') : $t('general.no') }}</UBadge>
                  </template>
                </td>
                <td class="border px-4 py-2">
                  <template v-if="editingState[row.original.id]">
                    <UInput v-model="row.original.settings[index].startTime" type="time" />
                  </template>
                  <template v-else>
                    {{ setting.startTime }}
                  </template>
                </td>
                <td class="border px-4 py-2">
                  <template v-if="editingState[row.original.id]">
                    <UInput v-model="row.original.settings[index].endTime" type="time" />
                  </template>
                  <template v-else>
                    {{ setting.endTime }}
                  </template>
                </td>
                <td class="border px-4 py-2 text-center">
                  <template v-if="editingState[row.original.id]">
                    <UButton color="error" variant="subtle" icon="i-lucide-trash" @click="deleteSetting(row.original.id, index)" />
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
          <template v-if="editingState[row.original.id]">
            <UButton class="mt-4" variant="subtle" icon="i-lucide-plus" @click="addSetting(row.original.id)">
              {{ $t('service.addSetting') }}
            </UButton>
          </template>
        </template>
      </UTable>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'

const { t } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const UInput = resolveComponent('UInput')

const dateRangeItems = ref([
  {
    label: t('general.january'),
    value: '01-01'
  }, {
    label: t('general.february'),
    value: '02-01'
  }, {
    label: t('general.march'),
    value: '03-01'
  }, {
    label: t('general.april'),
    value: '04-01'
  }, {
    label: t('general.may'),
    value: '05-01'
  }, {
    label: t('general.june'),
    value: '06-01'
  }, {
    label: t('general.july'),
    value: '07-01'
  }, {
    label: t('general.august'),
    value: '08-01'
  }, {
    label: t('general.september'),
    value: '09-01'
  }, {
    label: t('general.october'),
    value: '10-01'
  }, {
    label: t('general.november'),
    value: '11-01'
  }, {
    label: t('general.december'),
    value: '12-01'
  }
])

const getLabelForValue = (value: string) => {
  const item = dateRangeItems.value.find(item => item.value === value)
  return item ? item.label : value
}

const listData = ref([])
const editingState = reactive<{ [key: string]: boolean }>({})
const editedRows = reactive<{ [key: string]: Service }>({})

type Service = {
  id: string
  servicename: string
  settings: [{
    rangeFrom: string
    rangeTo: string
    mandatoryAssignees: number
    saturdays: boolean
    sundays: boolean
    publicHolidays: boolean
    startTime: string
    endTime: string
  }]
}

const columns = computed<TableColumn<Service>[]>(() => [
  {
    id: 'expand',
    cell: ({ row }) =>
      h(UButton, {
        color: 'neutral',
        variant: 'ghost',
        icon: 'i-lucide-chevron-down',
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
            successMessage.value = t('service.updateSuccess')
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
          const { error } = await useAPI(`/service/delete/${row.original.id}`, {
            method: 'DELETE'
          })
          if (error.value) {
            errorMessage.value = t('service.deleteFailure')
          } else {
            successMessage.value = t('service.deleteSuccess')
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

const expanded = ref({})

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data, error } = await useAPI(`/service/list`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      listData.value = data.value
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const addSetting = (rowId: string) => {
  if (!editedRows[rowId]) {
    editedRows[rowId] = { ...listData.value.find(row => row.id === rowId) }
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
  middleware: ['authenticated', 'admin']
})
</script>