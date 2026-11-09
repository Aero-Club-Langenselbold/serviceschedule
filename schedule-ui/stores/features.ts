import { defineStore } from 'pinia'

interface Feature {
  id: string,
  description: string,
  active: boolean
}

export const useFeatureStore = defineStore('features', () => {
  const features = ref<Feature[]>([])

  async function fetchFeatures() {
    const response = await useAPI('/featureflag/active', { method: 'GET' })
    const fetched = response.data.value

    // A failed request has no data: keep the last known flags instead of storing "undefined".
    if (!Array.isArray(fetched)) {
      return
    }

    features.value = fetched as Feature[]

    if (import.meta.client) {
      localStorage.setItem('features', JSON.stringify(features.value))
    }
  }

  async function validateFeatures() {
    if (import.meta.client) {
      const storedFeatures = localStorage.getItem('features')
      if (!storedFeatures) {
        return
      }

      try {
        const parsed = JSON.parse(storedFeatures)
        if (Array.isArray(parsed)) {
          features.value = parsed
          return
        }
      } catch {
        // handled below: older versions could store the text "undefined"
      }
      localStorage.removeItem('features')
    }
  }

  return { features, fetchFeatures, validateFeatures }
})
