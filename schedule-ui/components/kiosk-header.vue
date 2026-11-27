<template>
  <header class="sticky top-0 z-50 border-b border-default bg-default/95 backdrop-blur" data-kiosk-header>
    <div class="mx-auto flex max-w-6xl items-center gap-3 px-4 pt-3 sm:px-6">
      <NuxtLink :to="KIOSK_HOME" class="shrink-0" :aria-label="$t('kiosk.home')">
        <img :src="logo" :alt="$t('app.logoAlt')" class="h-12 w-auto" data-club-logo>
      </NuxtLink>
      <UBadge color="primary" variant="subtle" size="lg" icon="i-lucide-tablet-smartphone" :label="$t('kiosk.badge')" />

      <div class="ml-auto flex min-w-0 items-center gap-2">
        <span class="hidden min-w-0 truncate text-sm text-muted md:inline" data-kiosk-user>{{ user?.username }}</span>
        <UDropdownMenu :items="languages">
          <UButton color="neutral" variant="ghost" size="xl" :aria-label="$t('nav.language')" :icon="locale === 'en' ? 'circle-flags:us' : 'circle-flags:de'" />
        </UDropdownMenu>
        <UButton color="neutral" variant="outline" size="xl" icon="i-lucide-log-out" :label="$t('kiosk.logout')" data-kiosk-logout @click="signOut" />
      </div>
    </div>

    <!-- large touch targets, wrap on narrow tablets so every area stays visible -->
    <nav class="mx-auto max-w-6xl px-4 py-3 sm:px-6" :aria-label="$t('kiosk.navigation')">
      <ul class="flex flex-wrap gap-2">
        <li v-for="item in KIOSK_DESTINATIONS" :key="item.to" class="shrink-0">
          <UButton
            :to="item.to"
            size="xl"
            :icon="item.icon"
            :label="$t(item.label)"
            :color="active === item.to ? 'primary' : 'neutral'"
            :variant="active === item.to ? 'solid' : 'soft'"
            :aria-current="active === item.to ? 'page' : undefined"
            class="whitespace-nowrap"
          />
        </li>
      </ul>
    </nav>
  </header>
</template>

<script lang="ts" setup>
import logo from '~/assets/logo.png'

// Header of the kiosk mode (shared tablet): only the areas a kiosk session reaches and a prominent sign out.
// No bug list, notifications or personal settings.
const route = useRoute()
const { setLocale, locale } = useI18n()
const { setUserLocale } = useLocaleStore()
const userStore = useUserStore()
const { logout } = userStore
const { user } = storeToRefs(userStore)

const active = computed(() => activeKioskDestination(route.path))

const signOut = async () => {
  await logout()
  await navigateTo('/login')
}

const changeLocale = (code: 'de' | 'en') => {
  setUserLocale(code)
  setLocale(code)
}

const languages = computed(() => [[
  { label: 'Deutsch', icon: 'circle-flags:de', onSelect: () => changeLocale('de') },
  { label: 'English', icon: 'circle-flags:us', onSelect: () => changeLocale('en') }
]])
</script>
