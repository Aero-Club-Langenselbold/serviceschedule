<template>
  <UHeader to="/" mode="slideover" :ui="{ root: 'bg-default/95' }">
    <template #title>
      <span class="flex items-center gap-2.5">
        <span class="flex size-8 items-center justify-center rounded-lg bg-primary text-inverted">
          <UIcon name="i-lucide-calendar-check" class="size-5" />
        </span>
        <span class="text-base font-semibold text-highlighted">{{ $t('app.shortName') }}</span>
      </span>
    </template>

    <UNavigationMenu v-if="menu.length" :items="menu" orientation="horizontal" content-orientation="vertical" :ui="{ linkLeadingIcon: 'size-4' }" />

    <template #right>
      <UDropdownMenu :items="languages">
        <UButton color="neutral" variant="ghost" :aria-label="$t('nav.language')" :icon="locale === 'en' ? 'circle-flags:us' : 'circle-flags:de'" />
      </UDropdownMenu>

      <UDropdownMenu v-if="isLoggedIn" :items="account" :content="{ align: 'end' }">
        <UButton color="neutral" variant="ghost" icon="i-lucide-circle-user" :aria-label="$t('nav.account')">
          <span class="hidden max-w-44 truncate xl:inline">{{ user?.username }}</span>
        </UButton>
      </UDropdownMenu>
      <UButton v-else-if="route.path !== '/login'" to="/login" icon="i-lucide-log-in" color="neutral" variant="ghost" :label="$t('nav.login')" />
    </template>

    <template #body>
      <UNavigationMenu :items="menu" orientation="vertical" class="-mx-2" />
    </template>

    <template v-if="!menu.length" #toggle>
      <span />
    </template>
  </UHeader>
</template>

<script lang="ts" setup>
const route = useRoute()
const { t, setLocale, locale } = useI18n()
const localeStore = useLocaleStore()
const switchLocalePath = useSwitchLocalePath()
const { setUserLocale } = localeStore
const userStore = useUserStore()
const { logout } = userStore
const { isLoggedIn, user } = storeToRefs(userStore)

const hasRole = (...roles: string[]) => roles.some(role => user.value?.roles.includes(role))

const menu = computed(() => {
  if (!isLoggedIn.value) {
    return []
  }
  const items: any[] = [{
    label: t('nav.home'),
    icon: 'i-lucide-house',
    to: '/'
  }]
  if (hasRole('ROLE_SCHEDULE_ADMIN', 'ROLE_ADMIN')) {
    items.push({
      label: t('nav.participants.menu'),
      icon: 'i-lucide-users',
      defaultOpen: true,
      children: [
        { label: t('nav.participants.list'), icon: 'i-lucide-list', to: '/participants/list' },
        { label: t('nav.participants.add'), icon: 'i-lucide-user-plus', to: '/participants/add' },
        { label: t('nav.participants.import'), icon: 'i-lucide-upload', to: '/participants/import' }
      ]
    }, {
      label: t('nav.service.menu'),
      icon: 'i-lucide-briefcase',
      defaultOpen: true,
      children: [
        { label: t('nav.service.list'), icon: 'i-lucide-list', to: '/service/list' },
        { label: t('nav.service.add'), icon: 'i-lucide-plus', to: '/service/add' }
      ]
    }, {
      label: t('nav.plan.menu'),
      icon: 'i-lucide-calendar-days',
      defaultOpen: true,
      children: [
        { label: t('nav.plan.list'), icon: 'i-lucide-list', to: '/plan/list' },
        { label: t('nav.plan.add'), icon: 'i-lucide-plus', to: '/plan/add' }
      ]
    })
  } else if (hasRole('ROLE_USER')) {
    items.push({
      label: t('nav.plan.usermenu'),
      icon: 'i-lucide-calendar-days',
      to: '/plan/list'
    })
  }
  if (hasRole('ROLE_ALARM_ADMIN', 'ROLE_ADMIN')) {
    items.push({
      label: t('nav.alarms.menu'),
      icon: 'i-lucide-bell',
      defaultOpen: true,
      children: [
        { label: t('nav.alarms.list'), icon: 'i-lucide-list', to: '/alarm/list' }
      ]
    })
  }
  return items
})

const account = computed(() => [
  [{ label: user.value?.username, type: 'label' as const }],
  [
    ...(hasRole('ROLE_ADMIN') ? [{ label: t('nav.settings'), icon: 'i-lucide-settings', to: '/settings' }] : []),
    { label: t('nav.requests'), icon: 'i-lucide-bug', to: '/request/list' }
  ],
  [{
    label: t('nav.logout'),
    icon: 'i-lucide-log-out',
    onSelect: async () => {
      await logout()
      navigateTo('/login')
    }
  }]
])

const changeLocale = (code: 'de' | 'en') => {
  setUserLocale(code)
  setLocale(code)
  navigateTo(switchLocalePath(code))
}

const languages = computed(() => [[
  { label: 'Deutsch', icon: 'circle-flags:de', onSelect: () => changeLocale('de') },
  { label: 'English', icon: 'circle-flags:us', onSelect: () => changeLocale('en') }
]])
</script>
