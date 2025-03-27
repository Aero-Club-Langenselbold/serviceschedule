<template>
  <div>
    <NavMenu />
  </div>
  <div class="flex items-center justify-center min-h-screen">
    <div class="p-8 rounded-4xl shadow-md w-full max-w-md border-2">
      <h2 class="text-2xl font-bold mb-6 text-center">{{ $t('settings.title') }}</h2>
      <form @submit.prevent="onSubmit">
        <Messages :errorMessage="errorMessage" :successMessage="successMessage" />
        <div class="mb-4">
          <label class="block text-sm font-bold mb-2">
            {{ $t('settings.holidayInfo') }}
          </label>
        </div>
        <div class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('settings.country') }}
          </label>
          <USelect v-model="selectedCountryId" value-key="id" :items="availableCountries" class="w-full" @update:modelValue="fetchAvailableStates" />
        </div>
        <div v-if="availableStates.length > 0" class="mb-6">
          <label class="block text-sm font-bold mb-2">
            {{ $t('settings.state') }}
          </label>
          <USelect v-model="selectedStateId" value-key="id" :items="availableStates" class="w-full" />
        </div>
        <div v-if="selectedStateId" class="text-center mt-4">
          <UButton trailing-icon="i-lucide-arrow-right" size="md" :label="$t('settings.save')" type="submit" />
        </div>
      </form>
    </div>
  </div>
</template>

<script lang="ts" setup>
const { t } = useI18n()
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
import type { Country } from '~/types/country'
import type { State } from '~/types/state'
import type { SelectedState } from '~/types/selectedState'

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

const fetchData = async () => {
  try {
    await fetchAvailableCountries()
    const { data: ss, error: ess } = await useAPI<SelectedState>(`/settings/selected-state`, { method: 'GET' })
    if (ess.value) {
      errorMessage.value = ess.value.message
    } else if (ss.value) {
      if (ss.value.country.id) {
        selectedCountryId.value = ss.value.country.id
        await fetchAvailableStates(selectedCountryId.value)
      }
      selectedStateId.value = ss.value.state.id
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
      if (response.status.value === "error") {
        errorMessage.value = 'An error occurred while saving the data.'
        successMessage.value = t('settings.success')
        errorMessage.value = null
      }
    })
    .catch((error) => {
      errorMessage.value = 'An error occurred while saving the data.'
    })
  }
}

watchEffect(() => {
  fetchData()
})

definePageMeta({
  middleware: ['authenticated', 'admin']
})
</script>