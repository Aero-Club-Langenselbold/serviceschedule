<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-6 mb-6">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('request.title') }}</h2>

      <UModal :title="$t('request.add.title')" :dismissible="false" v-model:open="modalOpen">
        <UButton>
            {{ $t('request.add.open') }}
        </UButton>

        <template #body>
          <form @submit.prevent="onSubmit">
            <div class="mb-4">
              <label class="block text-sm font-bold mb-2">
                {{ $t('request.headline') }}
              </label>
              <UInput v-model="request.headline" type="text" :placeholder="$t('request.headline')" class="w-full" />
            </div>
            <div class="mb-4">
              <label class="block text-sm font-bold mb-2">
                {{ $t('request.description') }}
              </label>
                <UTextarea v-model="request.description" color="primary" variant="subtle" :placeholder="$t('request.description')" autoresize class="w-full" />
            </div>
            <div class="mb-6 flex items-center ">
              <label class="text-sm font-bold mr-6">
                {{ $t('request.type.name') }}
              </label>
              <URadioGroup v-model="request.type" color="primary" variant="table" default-value="BUG" :items="typeItems" />
            </div>
            <div class="mb-6 flex items-center ">
              <label class="text-sm font-bold mr-6">
                {{ $t('request.status.name') }}
              </label>
              <USelect v-model="request.status" value-key="value" :items="statusItems" class="w-full" />
            </div>
            <div class="flex justify-end gap-4">
              <UButton color="success"  variant="solid"  :disabled="!request.headline || !request.description"  @click="addRequest()">
                {{ $t('request.add.submit') }}
              </UButton>
              <UButton color="neutral" variant="outline" @click="closeModal()">
                {{ $t('request.add.cancel') }}
              </UButton>
            </div>
          </form>
        </template>
      </UModal>

      <UTable v-model:expanded="expanded" :ui="{ tr: 'data-[expanded=true]:bg-(--ui-bg-elevated)/50' }" :data="listData?.content" :columns="columns" class="flex-1" >
        <template #expanded="{ row }">
          <div class="p-4">
            <UTextarea v-model="row.original.description" color="primary" variant="subtle" :placeholder="$t('request.description')" autoresize class="w-full" disabled />
          </div>
        </template>
      </UTable>
      <UPagination v-model:page="page" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData"/>

    </div>
  </div>
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
    accessorKey: 'headline',
    header: t('request.headline'),
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
        CLOSED: 'secondary' as const,
        REJECTED: 'error' as const
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
    cell: ({ row }) => (row.getValue('author') as Participant).fullname
  }, {
    id: 'edit',
    cell: ({ row }) =>
      h(UButton, {
        color: 'primary',
        variant: 'subtle',
        icon: 'i-lucide-edit',
        onClick: () => openEditModal(row.original)
      }, t('request.edit'))
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