<template>
  <AppPage :title="$t('nav.plan.usermenu')" :error-message="errorMessage" :success-message="successMessage">
    <template v-if="isAdmin" #actions>
      <UButton to="/plan/add" icon="i-lucide-plus" :label="$t('nav.plan.add')" />
    </template>

    <p v-if="!listData.length" class="rounded-xl border border-dashed border-accented p-8 text-center text-muted">
      {{ $t('common.empty') }}
    </p>

    <ul class="grid gap-4 lg:grid-cols-2">
      <li v-for="plan in listData" :key="plan.id">
        <article class="flex h-full flex-col gap-4 rounded-xl border border-default bg-default p-4 sm:p-5">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <h2 class="text-lg font-semibold text-highlighted">{{ plan.name }}</h2>
              <p v-if="plan.description" class="mt-0.5 text-sm text-muted">{{ plan.description }}</p>
            </div>
            <UBadge :color="stateOf(plan).color" variant="subtle" class="shrink-0">{{ stateOf(plan).label }}</UBadge>
          </div>
          <p class="flex items-center gap-2 text-sm text-default">
            <UIcon name="i-lucide-calendar" class="size-4 shrink-0 text-muted" />
            {{ formatDate(plan.dateFrom) }} – {{ formatDate(plan.dateUntil) }}
          </p>
          <div class="mt-auto grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
            <UTooltip v-for="action in actionsFor(plan)" :key="action.label" :delay-duration="0" :text="action.tooltip">
              <UButton
                :label="action.label"
                :icon="action.icon"
                :disabled="action.disabled"
                :loading="action.loading"
                :variant="action.primary ? 'solid' : 'outline'"
                :color="action.primary ? 'primary' : 'neutral'"
                class="w-full sm:w-auto"
                @click="action.onClick"
              />
            </UTooltip>
            <ConfirmationDialog
              v-if="isAdmin"
              color="error"
              variant="ghost"
              icon="i-lucide-trash"
              :loading="deleting === plan.id"
              :button-label="$t('plan.actionLabels.delete')"
              :tooltip="$t('plan.deleteInfo')"
              :modal-title="$t('plan.deleteConfirmationTitle')"
              :confirm-label="$t('plan.deleteConfirmation')"
              :abort-label="$t('plan.cancel')"
              button-class="w-full sm:w-auto"
              :on-confirm="() => deletePlan(plan.id)"
            />
          </div>
        </article>
      </li>
    </ul>
  </AppPage>
</template>

<script lang="ts" setup>
import iconv from 'iconv-lite'
import type { Plan } from '~/types/plan'
import type { Service } from '~/types/service'

const { t, locale } = useI18n()

const userStore = useUserStore()
const { user } = storeToRefs(userStore)

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const listData = ref<Plan[]>([])

const isAdmin = computed(() => !!user.value?.roles.includes('ROLE_ADMIN'))

const formatDate = (date: Date | null) => date
  ? new Date(date).toLocaleDateString(locale.value, { day: '2-digit', month: '2-digit', year: 'numeric' })
  : ''

const stateOf = (plan: Plan) => {
  if (plan.published) {
    return { label: t('plan.state.published'), color: 'success' as const }
  }
  return plan.serviceDates.length > 0
    ? { label: t('plan.state.open'), color: 'primary' as const }
    : { label: t('plan.state.draft'), color: 'neutral' as const }
}

const actionsFor = (plan: Plan) => {
  const hasServiceDates = plan.serviceDates.length > 0
  const finalPlan = {
    label: t('plan.actionLabels.final'),
    tooltip: t('plan.generatedDetailsInfo'),
    icon: 'i-lucide-book-open',
    disabled: !plan.published,
    primary: plan.published,
    onClick: () => navigateTo(`/plan/generated/${plan.id}`)
  }
  const pdf = {
    label: t('plan.actionLabels.pdf'),
    tooltip: t('plan.pdfInfo'),
    icon: 'i-lucide-file-down',
    disabled: !plan.published,
    onClick: () => downloadPdf(plan.id)
  }

  if (!isAdmin.value) {
    return [{
      label: t('plan.actionLabels.myAvailability'),
      tooltip: t('plan.availabilityInfo'),
      icon: 'i-lucide-calendar-check',
      disabled: !hasServiceDates || plan.published,
      primary: hasServiceDates && !plan.published,
      onClick: () => navigateTo(`/plan/availabilities/me/${plan.id}`)
    }, finalPlan, pdf]
  }

  return [{
    label: t('plan.actionLabels.details'),
    tooltip: t('plan.detailsInfo'),
    icon: 'i-lucide-pencil',
    disabled: plan.published,
    primary: !hasServiceDates,
    onClick: () => navigateTo(`/plan/details/${plan.id}`)
  }, {
    label: t('plan.actionLabels.availabilities'),
    tooltip: t('plan.availabilityInfo'),
    icon: 'i-lucide-users',
    disabled: !hasServiceDates,
    onClick: () => navigateTo(`/plan/availabilities/${plan.id}`)
  }, {
    label: t('plan.actionLabels.generate'),
    tooltip: t('plan.generateInfo'),
    icon: 'i-lucide-sparkles',
    disabled: !hasServiceDates,
    primary: hasServiceDates && !plan.published,
    loading: generating.value === plan.id,
    onClick: () => generatePlan(plan.id)
  }, finalPlan, {
    label: t('plan.actionLabels.statistics'),
    tooltip: t('plan.statisticsInfo'),
    icon: 'i-lucide-chart-bar',
    disabled: !plan.published,
    onClick: () => navigateTo(`/plan/statistics/${plan.id}`)
  }, pdf, {
    label: t('plan.actionLabels.csv'),
    tooltip: t('plan.csvInfo'),
    icon: 'i-lucide-file-text',
    disabled: !plan.published,
    onClick: () => downloadCsv(plan.id)
  }]
}

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data, error } = await useAPI<Plan[]>(`/plan/list`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      listData.value = data.value ?? []
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const generating = ref<string | null>(null)
const deleting = ref<string | null>(null)
const generatePlan = async (id: string) => {
  errorMessage.value = null
  successMessage.value = null
  generating.value = id

  try {
    const { error } = await useAPI(`/plan/generate/${id}`, { method: 'POST' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      await fetchData()
      successMessage.value = 'Plan generated successfully.'
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while generating the plan.'
  } finally {
    generating.value = null
  }
}

const downloadPdf = async (id: string) => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data, error } = await useAPI(`/plan/download/pdf/${id}`, { method: 'GET' })
    if (error.value) {
      errorMessage.value = error.value.message
    }

    const file = window.URL.createObjectURL(data.value as Blob)
    window.open(file, '_blank')
  } catch (err) {
    errorMessage.value = 'An error occurred while downloading the PDF.'
  }
}

const downloadCsv = async (id: string) => {
  errorMessage.value = null
  successMessage.value = null

  try {
    const { data: serviceData, error: serviceError } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })

    if (serviceError?.value?.message === 'error') {
      errorMessage.value = "Unable to fetch services."
      return
    }

    serviceData.value?.forEach(async (service) => {
      const { data, error } = await useAPI(`/plan/download/csv/${id}/${service.id}`, { method: 'GET' })
      if (error.value) {
        errorMessage.value = error.value.message
      }

      // Convert the data to ISO-8859-1 using iconv-lite
      const csvContent = data.value as string // Assuming the API returns CSV content as a string
      const isoEncodedContent = iconv.encode(csvContent, 'ISO-8859-1')

      const blob = new Blob([isoEncodedContent], { type: 'text/csv;charset=ISO-8859-1' })
      const link = document.createElement('a')
      link.href = window.URL.createObjectURL(blob)
      link.download = `${service.servicename}.csv`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    })
  } catch (err) {
    errorMessage.value = 'An error occurred while downloading the PDF.'
  }
}

const deletePlan = async (id: string) => {
  errorMessage.value = null
  successMessage.value = null
  deleting.value = id

  try {
    const { error } = await useAPI(`/plan/delete/${id}`, { method: 'DELETE' })
    if (error.value) {
      errorMessage.value = error.value.message
    } else {
      await fetchData()
      successMessage.value = 'Plan deleted successfully.'
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while deleting the plan.'
  } finally {
    deleting.value = null
  }
}

watchEffect(() => {
  fetchData()
})


definePageMeta({
  middleware: ['authenticated']
})
</script>