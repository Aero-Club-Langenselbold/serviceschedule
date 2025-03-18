<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-full border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('availabilities.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <!-- Show loading spinner -->
      <div v-if="loading" class="flex justify-center items-center">
        <svg class="animate-spin h-8 w-8 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
        </svg>
      </div>

      <!-- Render table only when data is loaded -->
      <UTable v-else sticky :data="data" :columns="columns" class="flex-1" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'

const route = useRoute()
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const id = route.params.id
const loading = ref(true)

type Participant = {
  id: string
  firstname: string
  lastname: string
  status: 'ACTIVE' | 'INACTIVE'
  services: []
}

const participants = ref<Participant[]>([])

const data = ref<String[]>([])

const columns = computed<TableColumn<Participant>[]>(() => {
  // Base columns (e.g., static columns)
  const baseColumns: TableColumn<Participant>[] = [
    {
      accessorKey: 'id',
      header: () => h('th', { class: 'rotate' }, [h('span', '')]),
      cell: ({ row }) => `${row.original}`
    }
  ]

  // Dynamically generate columns for each participant
  const participantColumns: TableColumn<Participant>[] = participants.value?.map(participant => ({
    accessorKey: participant.id, // Use participant ID as the key
    header: () => h('th', { class: 'rotate' }, [h('span', `${participant.lastname}, ${participant.firstname}`)]),
    cell: ({ row }) => {
      // Customize the cell content if needed
      return row.getValue(participant.id) || '-'
    }
  }))

  // Combine base columns with participant columns
  return participantColumns ? [...baseColumns, ...participantColumns] : baseColumns
})

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  try {
    const { data: participantsData, error: participantError } = await useAPI(`/participant/list`, { method: 'GET' })
    const { data: planData, error: planError } = await useAPI(`/plan/fetch/` + id, { method: 'GET' })
    if (participantError.value || planError.value) {
      errorMessage.value = "error while fetching data"
    } else {
      participants.value = participantsData.value.map((participant: Participant) => {
        return {
          id: participant.id,
          firstname: participant.firstname,
          lastname: participant.lastname,
          status: participant.status,
          services: participant.services
        }
      })

      planData.value.serviceDates.sort((a: any, b: any) => new Date(a.date) - new Date(b.date))
      // Eliminate duplicate entries and
      const uniqueDates = new Set<string>()
      planData.value.serviceDates.forEach((serviceDate: any) => {
        uniqueDates.add(serviceDate.date)
      })
      data.value = Array.from(uniqueDates)
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  } finally {
    loading.value = false
  }
}

watchEffect(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated', 'admin']
})
</script>