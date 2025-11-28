// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  modules: [
    '@nuxt/ui',
    '@nuxtjs/i18n',
    'nuxt-auth-utils',
    '@nuxt/icon',
    '@pinia/nuxt',
    '@nuxt/test-utils/module'
  ],
  i18n: {
    locales: [
      {
        code: 'en',
        file: 'en-US.ts'
      },
      {
        code: 'de',
        file: 'de-DE.ts'
      }
    ],
    strategy: 'no_prefix',
    defaultLocale: 'de',
    detectBrowserLanguage: {
      useCookie: true,
      alwaysRedirect: true
    }
  },
  css: ['~/assets/css/main.css'],
  ssr: false,
  runtimeConfig: {
      public: {
          API_URL: process.env.API_URL ?? 'http://localhost:8080/api/v1/'
      }
  },
  experimental: {
    granularCachedData: false,
    purgeCachedData: false,
  }
})