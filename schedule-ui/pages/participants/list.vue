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

const UBadge = resolveComponent('UBadge')

type Participant = {
  firstname: string
  lastname: string
  email: string
  dateOfBirth: string
  status: 'ACTIVE' | 'PASSIVE'
  services: string[]
}

const columns = computed<TableColumn<Participant>[]>(() => [
  {
    accessorKey: 'lastname',
    header: t('participants.lastname')
  }, {
    accessorKey: 'firstname',
    header: t('participants.firstname')
  }, {
    accessorKey: 'email',
    header: t('participants.email')
  }, {
    accessorKey: 'dateOfBirth',
    header: t('participants.dateOfBirth'),
    cell: ({ row }) => {
      return new Date(row.getValue('dateOfBirth')).toLocaleString(locale.value, {
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
        PASSIVE: 'error' as const
      }[row.getValue('status') as string]

      return h(UBadge, { class: 'capitalize', variant: 'subtle', color }, () =>
        row.getValue('status') === 'ACTIVE' ? t('participants.active') : t('participants.passive')
      )
    }
  }, {
    accessorKey: 'services',
    header: t('participants.services'),
    cell: ({ row }) => {
      return row.getValue('services').map((service: { servicename: string }) => service.servicename).join(', ')
    }
  }
])

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data, error } = await useAPI(`/participant/page?page=${page.value - 1}&size=${pageSize.value}`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      listData.value = data.value.content
      totalItems.value = data.value.totalElements
    }
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