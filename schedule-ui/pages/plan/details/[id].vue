<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen mt-6 mb-6">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('plan.details') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
      <div class="flex justify-start mb-4 mt-4">
        <UButton color="neutral" variant="subtle" icon="i-lucide-arrow-left" @click="navigateTo('/plan/list')">
          {{ $t('plan.backToList') }}
        </UButton>
      </div>
      <div class="flex justify-end mb-4">
        <UPopover>
          <UButton color="neutral" variant="subtle" icon="i-lucide-calendar">
            {{ selectedDate ? df.format(selectedDate.toDate(getLocalTimeZone())) : t('plan.addDate') }}
          </UButton>

          <template #content>
            <UCalendar v-model="selectedDate" @update:modelValue="addDate" :is-date-unavailable="isDateUnavailable" class="p-2" />
          </template>
        </UPopover>
        <UButton color="primary" @click="saveDetails" class="ml-4" :loading="isSaving">{{ $t('plan.save') }}</UButton>
      </div>

      <Spinner v-if="loading" />

      <div v-else class="overflow-x-auto mt-4">
        <table class="min-w-full border border-gray-200 mt-4 ">
          <thead>
            <tr>
              <th class="px-4 py-2 border">{{ $t('plan.date') }}</th>
              <th v-for="service in services" :key="service.id" class="px-4 py-2 border">{{ service.servicename }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(date, dateIndex) in dates">
              <td class="border px-4 py-2">{{ date.localeString }}</td>
              <td v-for="(service, serviceIndex) in services" :key="service.id" class="border px-4 py-2">
                <UInput type="number" v-model="values[dateIndex][serviceIndex].mandatoryAssignees" class="w-15" />
                <UInput type="time" v-model="values[dateIndex][serviceIndex].startTime" class="w-half" />
                <UInput type="time" v-model="values[dateIndex][serviceIndex].endTime" class="w-half" />
              </td>
              <td class="border px-4 py-2 text-center">
                <UButton color="error" variant="subtle" icon="i-lucide-trash" @click="removeDate(dateIndex)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { DateValue } from '@internationalized/date'
import { CalendarDate, DateFormatter, getLocalTimeZone } from '@internationalized/date'
import type { Holiday } from '~/types/holiday'
import type { Plan } from '~/types/plan'
import type { Service } from '~/types/service'
import type { ServiceDate } from '~/types/serviceDate'

const df = new DateFormatter('en-US', {
  dateStyle: 'medium'
})

const route = useRoute()
const id = route.params.id
const { t, locale } = useI18n()
const loading = ref(true)

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const isSaving = ref<boolean>(false)
const selectedDate = shallowRef<CalendarDate | DateValue | undefined>(undefined)

const services = ref<any[]>([])
interface DateEntry {
  localeString: string;
  originalDate: Date;
}

const dates = ref<DateEntry[]>([])
const values = ref<{ id: string, mandatoryAssignees: number, startTime: string, endTime: string }[][]>([])
const plan = ref<any>({})

const isDateUnavailable = (date: DateValue) => {
  return dates.value.some(d =>  {
    const originalDate = new Date(d.originalDate)
    return originalDate.getDate() === date.day && originalDate.getMonth() + 1 === date.month && originalDate.getFullYear() === date.year
  })
}

const fetchData = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  values.value = []
  dates.value = []

  try {
    // fetch the plan details and all services
    const { data: planData, error: planError } = await useAPI<Plan>(`/plan/fetch/` + id, { method: 'GET' })
    const { data: servicesData, error: servicesError } = await useAPI<Service[]>(`/service/list`, { method: 'GET' })

    if (planError.value || servicesError.value) {
      errorMessage.value = 'An error occurred while fetching the data.'
      return
    }

    // if database entries exist load those
    if (planData.value && planData.value.serviceDates.length > 0) {
      buildTableData(planData, servicesData, true)
    } else {
      buildTableData(planData, servicesData, false)
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  } finally {
    loading.value = false
  }
}

const buildTableData = (planData: any, servicesData: any, fromDataBase: boolean) => {
  plan.value = planData.value
  services.value = servicesData.value

  if (fromDataBase) {
    createPageFromDB()
  } else {
    createNewPage()
  }
}

function createPageFromDB() {
  plan.value.serviceDates.sort((a: ServiceDate, b: ServiceDate) => new Date(a.date).getTime() - new Date(b.date).getTime())

  plan.value.serviceDates.forEach((entry: any) => {
      const existingDate = dates.value.find(date => date.originalDate.getTime() === new Date(entry.date).getTime())
      if (!existingDate) {
        dates.value.push({
          localeString: new Date(entry.date).toLocaleDateString(locale.value, { weekday: 'short', month: '2-digit', day: '2-digit', year: 'numeric' }),
          originalDate: new Date(entry.date)
        })
      }
    })
    values.value = dates.value.map(date =>
      services.value.map(service => {
        const entry = plan.value.serviceDates.find((e: any) =>
          new Date(e.date).getTime() === date.originalDate.getTime() && e.service.id === service.id
        )
        return entry ? {
          id: entry.id,
          mandatoryAssignees: entry.mandatoryAssignees,
          startTime: entry.startTime,
          endTime: entry.endTime
        } : {
          id: null,
          mandatoryAssignees: 0,
          startTime: '',
          endTime: ''
        }
      })
    )
}

const createNewPage = async () => {
  const startDate = new Date(plan.value.dateFrom)
  const endDate = new Date(plan.value.dateUntil)
  const currentDate = new Date(startDate)

  // consider holidays
  const { data: holidayData, error: holidayError } = await useAPI<Holiday[]>('/settings/holidays/' + id, { method: 'GET' })

  while (currentDate < endDate) {
    const isHoliday = holidayData.value?.some((holiday: any) => {
      const holidayDate = new Date(holiday)
      return holidayDate.toDateString() === currentDate.toDateString()
    })

    if (currentDate.getDay() === 6 || currentDate.getDay() === 0 || isHoliday) { // Saturday or Sunday or Holiday
      dates.value.push({
        localeString: currentDate.toLocaleDateString(locale.value, { weekday: 'short', month: '2-digit', day: '2-digit', year: 'numeric' }),
        originalDate: new Date(currentDate)
      })
    }
    currentDate.setDate(currentDate.getDate() + 1)
  }

  values.value = dates.value.map(date => services.value.map(service => {
    const s = getSettings(service, date.originalDate.toISOString())
      return s ? {
        id: '',
        mandatoryAssignees: s.mandatoryAssignees,
        startTime: s.startTime,
        endTime: s.endTime
      } : {
        id: '',
        mandatoryAssignees: 0,
        startTime: '',
        endTime: ''
      }
    })
  )
}

const getSettings = (service: any, date: string) => {
  const currentDate = new Date(date)
  const currentYear = currentDate.getFullYear()
  const isSaturday = currentDate.getDay() === 6
  const isSunday = currentDate.getDay() === 0
  const isHoliday = !isSaturday && !isSunday

  const s = service.settings.find((setting: any) => {
    let rangeFrom = new Date(`${currentYear}-${setting.rangeFrom}`)
    let rangeTo = new Date(`${currentYear}-${setting.rangeTo}`)
    if (rangeFrom.getTime() > rangeTo.getTime()) {
      rangeTo.setFullYear(currentYear + 1)
    }
    const isWithinRange = currentDate >= rangeFrom && currentDate < rangeTo
    const isValidDay = (isSaturday && setting.saturdays) || (isSunday && setting.sundays) || (isHoliday && setting.publicHolidays)
    return isWithinRange && isValidDay
  })

  return s
}

const addDate = () => {
  if (selectedDate.value) {
    const newDate = {
      localeString: selectedDate.value.toDate(getLocalTimeZone()).toLocaleDateString(locale.value, { weekday: 'short', month: '2-digit', day: '2-digit', year: 'numeric' }),
      originalDate: selectedDate.value.toDate(getLocalTimeZone())
    }

    // Check if the date already exists
    if (!dates.value.some(date => date.localeString === newDate.localeString)) {
      dates.value.push(newDate)
      dates.value.sort((a, b) => a.originalDate.getTime() - b.originalDate.getTime())

      // Initialize values for the new date
      const newValues = services.value.map(service => ({
        id: '',
        mandatoryAssignees: 0,
        startTime: service.settings.startTime || '',
        endTime: service.settings.endTime || ''
      }))
      values.value.splice(dates.value.findIndex(date => date.localeString === newDate.localeString), 0, newValues)
    }

    selectedDate.value = undefined
  }
}

const removeDate = (index: number) => {
  dates.value.splice(index, 1)
  values.value.splice(index, 1)
}

const saveDetails = async () => {
  isSaving.value = true
  try {
    const payload = dates.value.flatMap((date, dateIndex) =>
      services.value.map((service, serviceIndex) => {
        const { id, mandatoryAssignees, startTime, endTime } = values.value[dateIndex][serviceIndex]
        if (mandatoryAssignees > 0) {
          return {
            id: id,
            date: date.originalDate,
            service: {
              id: service.id
            },
            mandatoryAssignees,
            startTime,
            endTime
          }
        }
        return null
      }).filter(entry => entry !== null)
    )

    const { error } = await useAPI(`/plan/serviceDates/save/` + plan.value.id, { method: 'POST', body: JSON.stringify(payload) })

    if (error.value) {
      errorMessage.value = t('plan.saveError')
      return
    }
    successMessage.value = t('plan.saveSuccess')
  } catch (err) {
    errorMessage.value = t('plan.saveException')
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  fetchData()
})


definePageMeta({
    middleware: ['authenticated', 'admin']
})
</script>
