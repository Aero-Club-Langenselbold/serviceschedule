<template>
  <AppPage :title="$t('participants.import')" back="/participants/list" :back-label="$t('nav.participants.list')" :error-message="errorMessage" :success-message="successMessage" width="full">
    <form class="mb-6 flex flex-col gap-3 rounded-xl border border-default bg-default p-4 sm:flex-row sm:items-center" enctype="multipart/form-data" @submit.prevent="upload">
      <UInput type="file" accept=".csv" class="w-full sm:max-w-md" :aria-label="$t('participants.importFile')" v-on:change="onFileChange" />
      <UButton type="submit" icon="i-lucide-upload" :loading="saving">
        {{ $t('participants.import') }}
      </UButton>
    </form>

    <div v-if="imported">
      <DataTable :data="listData?.content" :columns="columns" />
      <div class="mt-4 flex justify-center">
        <UPagination v-model:page="page" :sibling-count="1" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData" />
      </div>
    </div>
  </AppPage>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Page } from '~/types/page'
import type { Participant } from '~/types/participant'
import type { ParticipantDiff } from '~/types/participantDiff'
import type { Service } from '~/types/service'

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const saving = ref(false)
const file = ref<File | null>(null)

const { t, locale } = useI18n()
const toast = useToast()

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const imported = ref(false)

const page = ref(1)
const pageSize = ref(20)
const listData = ref<Page<Participant> | null>(null)
const serviceItems = ref<Service[] | null>([])
const diff = ref<ParticipantDiff | null>(null)

const onFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target.files) {
    return
  }

  file.value = target.files[0]
}

const upload = async () => {
  saving.value = true
  try {
    if (!file.value) {
      errorMessage.value = 'Please select a file to upload'
      return
    }

    const formData = new FormData()
    formData.append('file', file.value)

    const { data: diffData, error: diffError } = await useAPI<ParticipantDiff>('/participant/import', { method: 'POST', body: formData })

    if (diffError.value) {
      errorMessage.value = 'An error occurred while uploading the file'
      return
    }
    diff.value = diffData.value
    errorMessage.value = null
    await fetchData()
    imported.value = true
  } catch (e) {
    errorMessage.value = 'An error occurred while uploading the file'
  } finally {
    saving.value = false
  }
}

const fetchData = async () => {
  const { data: participantData, error: participantError } = await useAPI<Page<Participant>>(`/participant/page?page=${page.value - 1}&size=${pageSize.value}`, { method: 'GET' })
  const { data: serviceData, error: serviceError } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })

  if (participantError.value && participantError.value?.statusCode !== 200) {
    errorMessage.value = participantError.value.data.message
  } else {
    listData.value = participantData.value
    if (page.value === 1) {
      addNewParticipants()
    }
  }
  serviceItems.value = serviceData.value
}

const columns = computed<TableColumn<Participant>[]>(() => {
  const baseColumns = [
  {
      accessorKey: 'acknowledge',
      header: '',
      cell: ({ row }: { row: { original: Participant, getValue: (key: string) => any } }) => {
        const email = row.original.email
        if (diff.value?.unchangedParticipants.some(participant => participant.email === email)) {
          return ''
        }
        if (diff.value?.updatedParticipants.some(participant => participant.email === email)) {
          return h(UButton, {
            'aria-label': t('common.acceptChange'),
            icon: 'i-lucide-save',
            onClick: async () => {
              try {
                const updatedParticipant = diff.value?.updatedParticipants.find(participant => participant.email === email)
                row.original.firstname = updatedParticipant?.firstname || row.original.firstname
                row.original.lastname = updatedParticipant?.lastname || row.original.lastname
                row.original.dateOfBirth = updatedParticipant?.dateOfBirth || row.original.dateOfBirth
                row.original.services = updatedParticipant?.services || row.original.services
                await useAPI('/participant/save', { method: 'POST', body: JSON.stringify(row.original), headers: { 'Content-Type': 'application/json' }})
                diff.value!.updatedParticipants = diff.value!.updatedParticipants.filter(participant => participant.email !== row.original.email)
                await fetchData()
                toast.add({
                  title: t('participants.updateSuccess'),
                  icon: 'i-lucide-circle-check',
                  color: 'primary'
                })
              } catch (error) {
                toast.add({
                  title: t('participants.updateFailure'),
                  icon: 'i-lucide-circle-x',
                  color: 'error'
                })
              }
            }
          })
        }
        if (diff.value?.newParticipants.some(participant => participant.email === email)) {
          return h(UButton, {
            'aria-label': t('common.addParticipant'),
            icon: 'i-lucide-plus',
            onClick: async () => {
              try {
                  await useAPI('/participant/save', { method: 'POST', body: JSON.stringify(row.original), headers: { 'Content-Type': 'application/json' }})
                  diff.value!.newParticipants = diff.value!.newParticipants.filter(participant => participant.email !== row.original.email)
                  await fetchData();
                  toast.add({
                    title: t('participants.importSuccess'),
                    icon: 'i-lucide-circle-check',
                    color: 'primary'
                  })
              } catch (error) {
                toast.add({
                  title: t('participants.importFailure'),
                  icon: 'i-lucide-circle-x',
                  color: 'error'
                })
              }
            }
          })
        }
        if (diff.value?.missingParticipants.some(participant => participant.email === email)) {
          return h(UButton, {
            'aria-label': t('common.removeParticipant'),
            color: 'error',
            icon: 'i-lucide-x',
            onClick: async () => {
              try {
                await useAPI(`/participant/delete/${row.original.id}`, { method: 'DELETE' })
                await fetchData()
                toast.add({
                  title: t('participants.deleteSuccess'),
                  icon: 'i-lucide-circle-check',
                  color: 'primary'
                })
              } catch (error) {
                toast.add({
                  title: t('participants.deleteFailure'),
                  icon: 'i-lucide-circle-x',
                  color: 'error'
                })
              }
            }
          })
        }
        return ''
      }
  }, {
      accessorKey: 'diff',
      header: '',
      meta: { cardTitle: true },
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const color = getRowHighlightColor(row.original.email)

        return h(UBadge, { variant: 'subtle', color }, () =>
            t('participants.diff.' + color)
          )
      }
    }, {
      accessorKey: 'lastname',
      header: t('participants.lastname'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const email = row.original.email
        const updatedParticipant = diff.value?.updatedParticipants.find(participant => participant.email === email)

        if (updatedParticipant) {
          // Compare values and show both old and new values
          const equal = updatedParticipant.lastname === row.getValue('lastname')

          if (!equal) {
            return h('div', { class: 'flex flex-col' }, [
              h('span', { class: 'text-muted line-through' }, row.getValue('lastname')), // Old value
              h('span', { class: 'font-semibold text-highlighted' }, updatedParticipant.lastname), // New value
            ])
          }
        }

        return row.getValue('lastname')
      }
    }, {
      accessorKey: 'firstname',
      header: t('participants.firstname'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const email = row.original.email;
        const updatedParticipant = diff.value?.updatedParticipants.find(participant => participant.email === email)

        if (updatedParticipant) {
          // Compare values and show both old and new values
          const equal = updatedParticipant.firstname === row.getValue('firstname')

          if (!equal) {
            return h('div', { class: 'flex flex-col' }, [
              h('span', { class: 'text-muted line-through' }, row.getValue('firstname')), // Old value
              h('span', { class: 'font-semibold text-highlighted' }, updatedParticipant.firstname), // New value
            ])
          }
        }

        return row.getValue('firstname')
      }
    }, {
      accessorKey: 'email',
      header: t('participants.email'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        return row.getValue('email')
      }
    }, {
      accessorKey: 'dateOfBirth',
      header: t('participants.dateOfBirth'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const email = row.original.email;
        const updatedParticipant = diff.value?.updatedParticipants.find(participant => participant.email === email)

        if (updatedParticipant) {
          // Compare values and show both old and new values
          const equal = updatedParticipant.dateOfBirth === row.getValue('dateOfBirth')

          if (!equal) {
            return h('div', { class: 'flex flex-col' }, [
              h('span', { class: 'text-muted line-through' }, new Date(row.getValue('dateOfBirth')).toLocaleString(locale.value, {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
              })), // Old value
              h('span', { class: 'font-semibold text-highlighted' }, updatedParticipant.dateOfBirth && new Date(updatedParticipant.dateOfBirth).toLocaleString(locale.value, {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
              })), // New value
            ])
          }
        }

        return new Date(row.getValue('dateOfBirth')).toLocaleString(locale.value, {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
          })
      }
    }, {
      accessorKey: 'status',
      header: t('participants.status'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const color = {
          ACTIVE: 'success' as const,
          INACTIVE: 'neutral' as const
        }[row.getValue('status') as string]

        return h(UBadge, { class: 'capitalize', variant: 'subtle', color }, () =>
            row.getValue('status') === 'ACTIVE' ? t('participants.active') : t('participants.inactive')
          )
      }
    }, {
      accessorKey: 'minAssignments',
      header: t('participants.minAssignments'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        return row.getValue('minAssignments')
      }
    }, {
      accessorKey: 'maxAssignments',
      header: t('participants.maxAssignments'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        return row.getValue('maxAssignments')
      }
    }, {
      accessorKey: 'services',
      header: t('participants.services'),
      cell: ({ row }: { row: { original: Participant; getValue: (key: string) => any } }) => {
        const email = row.original.email;
        const updatedParticipant = diff.value?.updatedParticipants.find(participant => participant.email === email)

        if (updatedParticipant) {
          // Compare values and show both old and new values
          const equal = updatedParticipant.participantServices?.map((ps) => ps.service.servicename).join(', ') === row.original.participantServices?.map((ps) => ps.service.servicename).join(', ')

          if (!equal) {
            return h('div', { class: 'flex flex-col' }, [
              h('span', { class: 'text-muted line-through' }, row.original.participantServices?.map((ps) => ps.service.servicename).join(', ')), // Old value
              h('span', { class: 'font-semibold text-highlighted' }, updatedParticipant.participantServices?.map((ps) => ps.service.servicename).join(', ')), // New value
            ])
          }
        }

        return row.original.participantServices?.map((ps) => ps.service.servicename).join(', ')
      }
    }
  ]

  return baseColumns
})

const getRowHighlightColor = (email: string): string => {
  if (diff.value?.unchangedParticipants.some(participant => participant.email === email)) {
    return 'success'
  }
  if (diff.value?.updatedParticipants.some(participant => participant.email === email)) {
    return 'warning'
  }
  if (diff.value?.missingParticipants.some(participant => participant.email === email)) {
    return 'error'
  }
  if (diff.value?.newParticipants.some(participant => participant.email === email)) {
    return 'secondary'
  }
  return 'success'
}


const addNewParticipants = () => {
  const newRows = diff.value?.newParticipants.map(participant => ({
    ...participant,
    isNew: true, // Add a flag to identify new participants
  })) || []

  if (listData.value?.content) {
    listData.value.content = [...newRows, ...listData.value.content];
  }
}

definePageMeta({
    middleware: ['authenticated', 'schedule-admin']
})
</script>