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
    vueI18n: './i18n.config.ts'
  },
  css: ['~/assets/css/main.css'],
  ssr: false,
  runtimeConfig: {
      public: {
          API_URL: process.env.API_URL ?? 'https://not-set.com'
      }
  }
})