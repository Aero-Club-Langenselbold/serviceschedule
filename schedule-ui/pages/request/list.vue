<template>
  <AppPage :title="$t('request.title')">
    <template #actions>
      <UModal :title="$t('request.add.title')" :dismissible="false" v-model:open="modalOpen">
        <UButton icon="i-lucide-plus">
          {{ $t('request.add.open') }}
        </UButton>

        <template #body>
          <form class="space-y-5" @submit.prevent="onSubmit">
            <UFormField :label="$t('request.headline')">
              <UInput v-model="request.headline" type="text" class="w-full" />
            </UFormField>
            <UFormField :label="$t('request.description')">
              <UTextarea v-model="request.description" :rows="3" autoresize class="w-full" />
            </UFormField>
            <UFormField :label="$t('request.type.name')">
              <URadioGroup v-model="request.type" color="primary" variant="table" default-value="BUG" :items="typeItems" />
            </UFormField>
            <UFormField :label="$t('request.status.name')">
              <USelect v-model="request.status" value-key="value" :items="statusItems" class="w-full" />
            </UFormField>
            <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <UButton color="neutral" variant="outline" @click="closeModal()">
                {{ $t('request.add.cancel') }}
              </UButton>
              <UButton :disabled="!request.headline || !request.description" @click="addRequest()">
                {{ $t('request.add.submit') }}
              </UButton>
            </div>
          </form>
        </template>
      </UModal>
    </template>

    <div class="@container">
      <DataTable v-model:expanded="expanded" :data="listData?.content" :columns="columns">
        <template #expanded="{ row }">
          <!-- The table can be wider than its scroll area, the details stay within the visible part. -->
          <div class="[overflow-wrap:anywhere] max-md:w-[calc(100cqw-2.125rem)] md:sticky md:left-4 md:w-[calc(100cqw-2.5rem)]">
            <p class="whitespace-pre-line rounded-lg bg-muted p-3 text-sm text-default md:mx-2">{{ row.original.description }}</p>
            <RequestComments :request-id="row.original.id" :status="row.original.status" />
          </div>
        </template>
      </DataTable>
    </div>
    <div class="mt-4 flex justify-center">
      <UPagination v-model:page="page" :sibling-count="1" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData" />
    </div>
  </AppPage>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Page } from '~/types/page'
import type { Participant } from '~/types/participant'
import type { Request } from '~/types/request'

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const { t, locale } = useI18n()
const userStore = useUserStore()
const { user } = storeToRefs(userStore)

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const modalOpen = ref(false)
const toast = useToast()
const expanded = ref({})

const page = ref(1)
const pageSize = ref(20)
const listData = ref<Page<Request> | null>(null)

const userData = ref<Participant | undefined | null>(undefined)
const request = ref<Request> ({
  id: '',
  headline: '',
  description: '',
  createdAt: new Date(),
  status: 'OPEN',
  type: 'BUG',
  author: userData.value
})

const statusItems = ref([
  {
    label: t('request.status.open'),
    value: 'OPEN'
  }, {
    label: t('request.status.in_progress'),
    value: 'IN_PROGRESS'
  }, {
    label: t('request.status.closed'),
    value: 'CLOSED'
  }, {
    label: t('request.status.rejected'),
    value: 'REJECTED'
  }
])

const closeModal = () => {
  modalOpen.value = false
  request.value = {
    id: '',
    headline: '',
    description: '',
    createdAt: new Date(),
    status: 'OPEN',
    type: 'BUG',
    author: userData.value
  }
}

const addRequest = async () => {
  request.value.author = userData.value
  const {data: requestData, error: requestError } = await useAPI<Participant>('/request/save', { method: 'POST', body: JSON.stringify(request.value) })
  if (requestError.value) {
    toast.add({
      title: t('request.add.error'),
      icon: 'i-lucide-bomb',
      color: 'error'
    })
  } else {
    toast.add({
      title: t('request.add.success'),
      icon: 'i-lucide-circle-check',
      color: 'primary'
    })
  }
  closeModal()
  fetchData()
}

const typeItems = ref([
  {
    label: t('request.type.bug'),
    value: 'BUG',
    description: t('request.type.bugdesc')
  },
  {
    label: t('request.type.feature'),
    value: 'FEATURE',
    description: t('request.type.featuredesc')
  }
])

const columns = computed<TableColumn<Request>[]>(() => [
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
    accessorKey: 'headline',
    header: t('request.headline'),
    meta: { cardTitle: true, class: { td: 'font-medium text-highlighted whitespace-normal' } },
    cell: ({ row }) => row.getValue('headline')
  }, {
    accessorKey: 'createdAt',
    header: t('request.createdAt'),
    cell: ({ row }) => {
      return new Date(row.getValue('createdAt')).toLocaleString(locale.value, {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric'
        })
    }
  }, {
    accessorKey: 'status',
    header: t('request.status.name'),
    cell: ({ row }) => {
      const color = {
        OPEN: 'success' as const,
        IN_PROGRESS: 'warning' as const,
        CLOSED: 'info' as const,
        REJECTED: 'neutral' as const
      }[row.getValue('status') as string]

      return h(UBadge, { class: 'capitalize', variant: 'subtle', color }, () => t('request.status.' + String(row.getValue('status'))?.toLowerCase()))
    }
  }, {
    accessorKey: 'type',
    header: t('request.type.name'),
    cell: ({ row }) => {
      const color = {
        BUG: 'error' as const,
        FEATURE: 'secondary' as const
      }[row.getValue('type') as string]

      return h(UBadge, { class: 'capitalize', variant: 'subtle', color }, () => t('request.type.' + String(row.getValue('type'))?.toLowerCase()))
    }
  }, {
    accessorKey: 'author',
    header: t('request.author'),
    cell: ({ row }) => (row.getValue('author') as Participant | null)?.fullname
  }, {
    id: 'edit',
    cell: ({ row }) =>
      h('div', { class: 'flex justify-end gap-2' }, [
        h(UButton, {
          'aria-label': t('common.toggleDetails'),
          color: 'neutral',
          variant: 'ghost',
          icon: row.getIsExpanded() ? 'i-lucide-chevron-up' : 'i-lucide-chevron-down',
          class: 'md:hidden mr-auto',
          onClick: () => row.toggleExpanded()
        }),
        h(UButton, {
          color: 'neutral',
          variant: 'outline',
          icon: 'i-lucide-pencil',
          onClick: () => openEditModal(row.original)
        }, () => t('request.edit'))
      ])
  }
])

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const {data: participantData, error: participantError } = await useAPI<Participant>('/participant/fetchByMail/' + user.value?.username, { method: 'GET' })
    const { data: requestData, error: requestError } = await useAPI<Page<Request>>(`/request/page?page=${page.value - 1}&size=${pageSize.value}`, { method: 'GET' })

    if (participantError.value || requestError.value) {
      errorMessage.value = "error while fetching data"
    } else {
      userData.value = participantData.value
      listData.value = requestData.value
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const openEditModal = (rowData: Request) => {
  // Populate the modal with the selected row's data
  request.value = { ...rowData }
  modalOpen.value = true
}

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null
}

onMounted(() => {
  fetchData()
})

definePageMeta({
    middleware: ['authenticated']
})
</script>