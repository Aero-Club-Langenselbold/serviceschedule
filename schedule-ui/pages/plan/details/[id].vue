<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen mt-6 mb-6">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-max border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('plan.details') }}</h2>
      <Messages :errorMessage="errorMessage" :successMessage="successMessage" />

      <div class="flex justify-end mb-4">
        <UPopover>
          <UButton color="neutral" variant="subtle" icon="i-lucide-calendar">
            {{ selectedDate ? df.format(selectedDate.toDate(getLocalTimeZone())) : t('plan.addDate') }}
          </UButton>

          <template #content>
            <UCalendar v-model="selectedDate" @update:modelValue="addDate" :is-date-unavailable="isDateUnavailable" class="p-2" />
          </template>
        </UPopover>
        <UButton color="primary" @click="saveDetails" class="ml-4">{{ $t('plan.save') }}</UButton>
      </div>

      <div class="overflow-x-auto mt-4">
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
import type {DateValue } from '@internationalized/date'
import { DateFormatter, getLocalTimeZone } from '@internationalized/date'

const df = new DateFormatter('en-US', {
  dateStyle: 'medium'
})

const route = useRoute()
const id = route.params.id
const { t, locale } = useI18n()

const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const selectedDate = shallowRef(null)

const services = ref<any[]>([])
interface DateEntry {
  localeString: string;
  originalDate: Date;
}

const dates = ref<DateEntry[]>([])
const values = ref<{ mandatoryAssignees: number, startTime: string, endTime: string }[][]>([])
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

  try {
    // fetch the plan details and all services
    const { data: planData, error: planError } = await useAPI(`/plan/fetch/` + id, { method: 'GET' })
    const { data: servicesData, error: servicesError } = await useAPI(`/service/list`, { method: 'GET' })

    if (planError?.value === 'error' || servicesError?.value === 'error') {
      errorMessage.value = 'An error occurred while fetching the data.'
      return
    }

    // TODO: if the plan already contains service_dates, build the table based on those instead of building a fresh table
    buildTableData(planData, servicesData)
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const buildTableData = (planData: any, servicesData: any) => {
  plan.value = planData.value
  services.value = servicesData.value

  const startDate = new Date(plan.value.dateFrom)
  const endDate = new Date(plan.value.dateUntil)
  const currentDate = new Date(startDate)

  while (currentDate < endDate) {
    if (currentDate.getDay() === 6 || currentDate.getDay() === 0) { // Saturday or Sunday
      dates.value.push({
        localeString: currentDate.toLocaleDateString(locale.value, { weekday: 'short', month: '2-digit', day: '2-digit', year: 'numeric' }),
        originalDate: new Date(currentDate)
      })
    }
    currentDate.setDate(currentDate.getDate() + 1)
  }

  values.value = dates.value.map(date =>
    services.value.map(service => {
      const s = getSettings(service, date.originalDate)
      return s ? {
        mandatoryAssignees: s.mandatoryAssignees,
        startTime: s.startTime,
        endTime: s.endTime
      } : {
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

  const s = service.settings.find((setting: any) => {
    const rangeFrom = new Date(`${currentYear}-${setting.rangeFrom}`)
    const rangeTo = new Date(`${currentYear}-${setting.rangeTo}`)
    const isWithinRange = currentDate >= rangeFrom && currentDate < rangeTo
    const isValidDay = (isSaturday && setting.saturdays) || (isSunday && setting.sundays)
    return isWithinRange && isValidDay
  })

  return s
}

const addDate = () => {
  if (selectedDate.value) {
    const newDate = {
      localeString: selectedDate.value.toDate().toLocaleDateString(locale.value, { weekday: 'short', month: '2-digit', day: '2-digit', year: 'numeric' }),
      originalDate: new Date(selectedDate.value)
    }

    // Check if the date already exists
    if (!dates.value.some(date => date.localeString === newDate.localeString)) {
      dates.value.push(newDate)
      dates.value.sort((a, b) => a.originalDate - b.originalDate)

      // Initialize values for the new date
      const newValues = services.value.map(service => ({
        mandatoryAssignees: 0,
        startTime: service.settings.startTime || '',
        endTime: service.settings.endTime || ''
      }))
      values.value.splice(dates.value.findIndex(date => date.localeString === newDate.localeString), 0, newValues)
    }

    selectedDate.value = null
  }
}

const removeDate = (index: number) => {
  dates.value.splice(index, 1)
  values.value.splice(index, 1)
}

const saveDetails = async () => {
  try {
    const payload = dates.value.flatMap((date, dateIndex) =>
      services.value.map((service, serviceIndex) => {
        const { mandatoryAssignees, startTime, endTime } = values.value[dateIndex][serviceIndex]
        if (mandatoryAssignees > 0) {
          return {
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

    const { data, error } = await useAPI(`/plan/serviceDates/save/` + plan.value.id, { method: 'POST', body: JSON.stringify(payload) })


    // TODO: reinitialize table with returned data
    if (error?.value === 'error') {
      errorMessage.value = 'An error occurred while saving the data.'
      return
    }

    successMessage.value = 'Data saved successfully.'
  } catch (err) {
    errorMessage.value = 'An error occurred while saving the data.'
  }
}

watchEffect(() => {
  fetchData()
})


definePageMeta({
    middleware: ['authenticated']
})
</script>
