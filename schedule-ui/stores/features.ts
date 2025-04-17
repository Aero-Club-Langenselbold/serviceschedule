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
    features.value = response.data.value as Feature[]

    if (import.meta.client) {
      localStorage.setItem('features', JSON.stringify(features.value))
    }
  }

  async function validateFeatures() {
    if (import.meta.client) {
        const storedFeatures = localStorage.getItem('features')
        if (storedFeatures) {
          features.value = JSON.parse(storedFeatures)
        }
    }
}

  return { features, fetchFeatures, validateFeatures }
})
