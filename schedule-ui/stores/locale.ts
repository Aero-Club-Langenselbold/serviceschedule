import { defineStore } from 'pinia'

export const useLocaleStore = defineStore('locale', () => {
    const userLocale = ref('de')

    async function setUserLocale(locale: string) {
      userLocale.value = locale
      localStorage.setItem('locale', userLocale.value)
    }

    async function validate() {
      const storedLocale = localStorage.getItem('locale')
      if (storedLocale) {
        userLocale.value = storedLocale
      } else {
        userLocale.value = 'de'
      }
    }

    function getUserLocale() {
      return userLocale.value
    }

    return { getUserLocale, setUserLocale, validate }
})