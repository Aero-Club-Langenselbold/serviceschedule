<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-start justify-center min-h-screen mt-4">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-8/12 border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('service.list') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <UTable :data="listData" :columns="columns" class="flex-1" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'

const UButton = resolveComponent('UButton')
const UTooltip = resolveComponent('UTooltip')

const { t, locale } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const listData = ref([])

type Plan = {
  id: string,
  name: string,
  description: string,
  dateFrom: string,
  dateUntil: string,
  published: boolean,
  serviceDates: string[]
}

const columns = computed<TableColumn<Plan>[]>(() => [
  {
    accessorKey: 'name',
    header: t('plan.name')
  }, {
    accessorKey: 'description',
    header: t('plan.description')
  }, {
    accessorKey: 'dateFrom',
    header: t('plan.dateFrom'),
    cell: ({ row }) => {
      return new Date(row.getValue('dateFrom')).toLocaleString(locale.value, {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    }
  }, {
    accessorKey: 'dateUntil',
    header: t('plan.dateUntil'),
    cell: ({ row }) => {
      return new Date(row.getValue('dateUntil')).toLocaleString(locale.value, {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    }
  }, {
    header: t('plan.actions'),
    cell: ({ row }) => {
      const hasServiceDates = row.original.serviceDates.length > 0;
      return [
        h(UTooltip, {
          'delay-duration': 0,
          text: t('plan.detailsInfo')
        }, {
          default: () =>
          h(UButton, {
            onClick: () => navigateTo(`/plan/details/${row.original.id}`),
            variant: hasServiceDates ? 'soft' : 'solid',
            color: hasServiceDates ? 'neutral' : 'primary',
            disabled: row.original.published,
            icon: "i-lucide-eye"
          })
        }),
        h(UTooltip, {
          'delay-duration': 0,
          text: t('plan.availabilityInfo'),
          class: 'ml-2'
        }, {
          default: () =>
          h(UButton, {
            onClick: () => navigateTo(`/plan/availabilities/${row.original.id}`),
            variant: hasServiceDates ? 'solid' : 'soft',
            color: hasServiceDates ? 'primary' : 'error',
            disabled: !hasServiceDates || row.original.published,
            icon: "i-lucide-users"
          })
        }),
        h(UTooltip, {
          'delay-duration': 0,
          text: t('plan.generateInfo'),
          class: 'ml-2'
        }, {
          default: () =>
          h(UButton, {
            onClick: () => navigateTo(`/plan/availabilities/${row.original.id}`),
            variant: hasServiceDates ? 'solid' : 'soft',
            color: hasServiceDates ? 'primary' : 'error',
            disabled: !hasServiceDates || row.original.published,
            icon: "i-lucide-plane"
          })
        }),
        h(UTooltip, {
          'delay-duration': 0,
          text: t('plan.generatedDetailsInfo'),
          class: 'ml-2'
        }, {
          default: () =>
          h(UButton, {
            onClick: () => navigateTo(`/plan/availabilities/${row.original.id}`),
            variant: row.original.published ? 'solid' : 'soft',
            color: row.original.published ? 'primary' : 'error',
            disabled: !row.original.published,
            icon: "i-lucide-book"
          })
        }),
        h(UTooltip, {
          'delay-duration': 0,
          text: t('plan.pdfInfo'),
          class: 'ml-2'
        }, {
          default: () =>
          h(UButton, {
            onClick: () => navigateTo(`/plan/availabilities/${row.original.id}`),
            variant: row.original.published ? 'solid' : 'soft',
            color: row.original.published ? 'primary' : 'error',
            disabled: !row.original.published,
            icon: "i-lucide-file-down"
          })
        })
    ]}
  }
])

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data, error } = await useAPI(`/plan/list`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      listData.value = data.value
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

watchEffect(() => {
  fetchData()
})


definePageMeta({
  middleware: ['authenticated', 'admin']
})
</script>