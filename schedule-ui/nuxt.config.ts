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
  // The app is light-only: without color mode there is no .dark class to activate.
  ui: {
    colorMode: false
  },
  // Ship the icons with the client, the static nginx deployment has no icon endpoint.
  icon: {
    clientBundle: {
      scan: true,
      sizeLimitKb: 1024
    }
  },
  app: {
    head: {
      meta: [
        { name: 'color-scheme', content: 'light only' },
        { name: 'theme-color', content: '#ffffff' }
      ]
    }
  },
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
