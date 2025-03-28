<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-full border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('availabilities.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />

      <div class="flex justify-start mb-4 mt-4">
        <UButton color="neutral" variant="subtle" icon="i-lucide-arrow-left" @click="navigateTo('/plan/list')">
          {{ $t('plan.backToList') }}
        </UButton>
      </div>

      <!-- Save button -->
      <div class="flex justify-end mb-4 mt-4">
        <UButton variant="solid" color="primary" icon="i-lucide-save" @click="savePreferences" :loading="saving" >
          {{ $t('availabilities.save') }}
        </UButton>
      </div>

      <Spinner v-if="loading" />

      <!-- Render table only when data is loaded -->
      <UTable v-else sticky :data="data" :columns="columns" class="flex-1 max-h-[600px]" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Participant } from '~/types/participant'
import type { ParticipantAvailability } from '~/types/participantAvailability'
import type { Plan } from '~/types/plan'
import type { ServiceDate } from '~/types/serviceDate'

const UButton = resolveComponent('UButton')

const route = useRoute()
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const id = route.params.id
const loading = ref(true)
const saving = ref(false)

const participants = ref<Participant[]>([])

const data = ref<String[]>([])

const columns = computed<TableColumn<any>[]>(() => {
  // First column: Dates
  const dateColumn: TableColumn<any> = {
    accessorKey: 'date',
    header: () => h('th', {}, [h('span', '')]),
    cell: ({ row }) => {
      const date = new Date(row.original.date)
      return date.toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: '2-digit', day: '2-digit' }) // Format date with locale
    }
  }

  // Participant columns: One column per participant
  const participantColumns: TableColumn<any>[] = participants.value?.map(participant => ({
    accessorKey: participant.id, // Use participant ID as the key
    header: () => h('th', { class: 'rotate' }, [h('span', `${participant.lastname}, ${participant.firstname}`)]),
    cell: ({ row }) => {
      // Toggle state for each cell
      const toggleStates = ['yes', 'no', 'please']

      const currentState = (row.getValue(participant.id) as { currentState: string }).currentState
      const preferencesSaved = (row.getValue(participant.id) as { preferencesSaved: boolean }).preferencesSaved

      const toggleState = () => {
        const currentIndex = toggleStates.indexOf(currentState)
        row.original[participant.id].currentState = toggleStates[(currentIndex + 1) % toggleStates.length]
        row.original[participant.id].edited = true
      }

      return h(UButton, {
        onClick: toggleState,
        variant: preferencesSaved ? 'solid' : 'subtle',
        color: currentState === 'yes' ? 'primary' : currentState === 'no' ? 'error' : 'secondary',
        icon: currentState === 'yes' ? 'i-lucide-thumbs-up' : currentState === 'no' ? 'i-lucide-thumbs-down' : 'i-lucide-plane'
      })
    }
  }))

  // Combine the date column with participant columns
  return [dateColumn, ...participantColumns]
})

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  try {
    const { data: participantsData, error: participantError } = await useAPI<Participant[]>(`/participant/list`, { method: 'GET' })
    const { data: planData, error: planError } = await useAPI<Plan>(`/plan/fetch/` + id, { method: 'GET' })
    const { data: availabilityData, error: availabilityError } = await useAPI<ParticipantAvailability[]>(`/plan/participantAvailability/fetch/${id}`, { method: 'GET' })
    if (participantError.value || planError.value || availabilityError.value) {
      errorMessage.value = "error while fetching data"
    } else if(participantsData.value) {
      participants.value = participantsData.value

      planData.value?.serviceDates.sort((a: ServiceDate, b: ServiceDate) => new Date(a.date).getTime() - new Date(b.date).getTime())
      // Eliminate duplicate entries
      const uniqueDates = new Set<string>()
      planData.value?.serviceDates.forEach((serviceDate: any) => {
        uniqueDates.add(serviceDate.date)
      })
      data.value = Array.from(uniqueDates).map(date => {
        const row: any = { date }
        participants.value?.forEach(participant => {
          const savedAvailability = (availabilityData.value ?? []).filter(av => av.participant.id === participant.id).length > 0
          let status = 'no'

          if (savedAvailability) {
            const savedDate = availabilityData.value?.find((availability: any) => availability.date === date && availability.participant.id === participant.id)
            if (savedDate) {
              status = savedDate.requested ? 'please' : 'yes'
            } else {
              status = 'no'
            }
          } else {
            status = participant.status === 'ACTIVE' ? 'yes' : 'no'
          }

          row[participant.id] = {
            currentState: status,
            preferencesSaved: savedAvailability,
            participant: participant,
            edited: savedAvailability
          }
        })
        return row
      })
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  } finally {
    loading.value = false
  }
}

const savePreferences = async () => {
  errorMessage.value = null
  successMessage.value = null
  saving.value = true

  try {
    const editedParticipants = data.value.flatMap((row: Record<string, any>) => {
      return participants.value.filter(participant => row[participant.id].edited)
    })
    const payload = data.value.flatMap((row: Record<string, any>) => {
      return participants.value.map(participant => {
        const participantData = row[participant.id];
        if (editedParticipants.some(editedParticipant => editedParticipant.id === participant.id) && (participantData.currentState === 'yes' || participantData.currentState === 'please')) {
          return {
            date: new Date(row.date),
            requested: participantData.currentState === 'please',
            participant: {
              id: participant.id
            },
            plan: {
              id: id
            }
          };
        }
        return null;
      }).filter(item => item !== null);
    })

    const { error } = await useAPI('/plan/participantAvailability/save/' + id, { method: 'POST', body: JSON.stringify(payload) })
    if (error.value) {
      errorMessage.value = 'An error occurred while saving the preferences.'
    } else {
      successMessage.value = 'Preferences saved successfully.'
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while saving the preferences.'
  } finally {
    saving.value = false
    fetchData()
  }
}

onMounted(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated', 'admin']
})
</script>