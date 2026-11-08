<template>
  <AppPage :title="$t('settings.title')" width="sm">
    <UCard :ui="{ root: 'rounded-xl' }">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <p class="text-sm text-muted">{{ $t('settings.holidayInfo') }}</p>
        <UFormField :label="$t('settings.country')">
          <USelect v-model="selectedCountryId" value-key="id" :items="availableCountries" class="w-full" @update:modelValue="fetchAvailableStates" />
        </UFormField>
        <UFormField v-if="availableStates.length > 0" :label="$t('settings.state')">
          <USelect v-model="selectedStateId" value-key="id" :items="availableStates" class="w-full" />
        </UFormField>
        <USeparator v-if="data.length" />
        <UFormField v-for="setting in data" :key="setting.settingKey" :label="$t('settings.keys.' + setting.settingKey)">
          <UInput v-model="setting.settingValue" class="w-full" />
        </UFormField>
        <UButton v-if="selectedStateId" block size="lg" :label="$t('settings.save')" type="submit" :loading="loading" />
      </form>
    </UCard>
  </AppPage>
</template>

<script lang="ts" setup>
import type { Country } from '~/types/country'
import type { SelectedState } from '~/types/selectedState'
import type { State } from '~/types/state'
const { t } = useI18n()
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const loading = ref(false)

type DisplayCountry = {
  id: string
  label: string
}
type DisplayState = {
  id: string
  label: string
}

const selectedCountryId = ref<string | undefined>(undefined)
const selectedStateId = ref<string | undefined>(undefined)
const availableCountries = ref<DisplayCountry[]>([])
const availableStates = ref<DisplayState[]>([])
const data = ref<any[]>([])

const fetchData = async () => {
  try {
    await fetchAvailableCountries()
    const { data: ss, error: ess } = await useAPI<SelectedState>(`/settings/selected-state`, { method: 'GET' })
    const { data: customSettingsData, error: customSettingsError } = await useAPI<any>(`/settings/common-settings`, { method: 'GET' })
    if (ess.value || customSettingsError.value) {
      errorMessage.value = 'Error fetching data'
    } else if (ss.value && customSettingsData.value) {
      if (ss.value.country.id) {
        selectedCountryId.value = ss.value.country.id
        await fetchAvailableStates(selectedCountryId.value)
      }
      selectedStateId.value = ss.value.state.id
      data.value = customSettingsData.value
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const fetchAvailableCountries = async () => {
  try {
    const { data: countries, error: countriesError } = await useAPI<Country[]>('/settings/countries', { method: 'GET' })
    if (countriesError.value) {
      errorMessage.value = countriesError.value.message
    } else if (countries.value) {
      availableCountries.value = countries.value.map((country: Country) => ({
        id: country.id,
        label: t('settings.countries.' + country.name.toLowerCase())
      }))
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const fetchAvailableStates = async (countryId: string) => {
  try {
    const { data: states, error: statesError } = await useAPI<State[]>(`/settings/states/${countryId}`, { method: 'GET' })
    if (statesError.value) {
      errorMessage.value = statesError.value.message
    } else if (states.value) {
      availableStates.value = states.value.map((state: State) => ({
        id: state.id,
        label: t('settings.states.' + state.name.toLowerCase().replace('-', '').replace(' ', ''))
      }))
    }
  } catch (err) {
    errorMessage.value = 'An error occurred while fetching the data.'
  }
}

const onSubmit = async () => {
  errorMessage.value = null
  successMessage.value = null
  loading.value = true

  // save common settings
  if (data.value.length > 0) {
    try {
      const { error: saveError } = await useAPI('/settings/common-settings', { method: 'POST', body: JSON.stringify(data.value) })
      if (saveError.value) {
        errorMessage.value = 'An error occurred while saving common settings.'
        loading.value = false
        return
      }
    } catch (err) {
      errorMessage.value = 'An error occurred while saving common settings.'
      loading.value = false
      return
    }
  }

  // save selected state
  if (selectedCountryId.value && selectedStateId.value) {
    const selectedStateData = {
      country: {
        id: selectedCountryId.value
      },
      state: {
        id: selectedStateId.value
      }
    }
    await useAPI('/settings/selected-state', { method: 'POST', body: JSON.stringify(selectedStateData) })
    .then(async (response) => {
      if (response.error?.value) {
        errorMessage.value = 'An error occurred while saving the data.'
      } else {
        successMessage.value = t('settings.success')
        errorMessage.value = null
      }
    })
    .catch((error) => {
      errorMessage.value = 'An error occurred while saving the data.'
    })
    .finally(() => {
      loading.value = false
    })
  }
}

watchEffect(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated', 'schedule-admin']
})
</script>