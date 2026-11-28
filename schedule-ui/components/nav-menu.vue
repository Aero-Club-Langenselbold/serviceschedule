<template>
  <UHeader to="/" mode="slideover" :ui="{ root: 'bg-default/95', center: 'lg:hidden xl:flex', toggle: 'lg:inline-flex xl:hidden' }">
    <template #title>
      <!-- club logo, leads home -->
      <img :src="logo" :alt="$t('app.logoAlt')" class="h-10 w-auto sm:h-12" data-club-logo>
    </template>

    <UNavigationMenu v-if="menu.length" :items="barMenu" orientation="horizontal" content-orientation="vertical" :ui="{ linkLeadingIcon: 'size-4' }" />

    <template #right>
      <UButton v-if="isImpersonating" color="warning" icon="i-lucide-user-round-x" :aria-label="$t('nav.stopImpersonation')" @click="leaveImpersonation">
        <span class="hidden whitespace-nowrap xl:inline">{{ $t('nav.stopImpersonation') }}</span>
      </UButton>

      <UDropdownMenu :items="languages">
        <UButton color="neutral" variant="ghost" :aria-label="$t('nav.language')" :icon="locale === 'en' ? 'circle-flags:us' : 'circle-flags:de'" />
      </UDropdownMenu>

      <NotificationBell v-if="isLoggedIn" />
      <UDropdownMenu v-if="isLoggedIn" :items="account" :content="{ align: 'end' }">
        <UButton color="neutral" variant="ghost" icon="i-lucide-circle-user" :aria-label="$t('nav.account')">
          <span class="hidden max-w-44 truncate 2xl:inline">{{ user?.username }}</span>
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
import logo from '~/assets/logo.png'

const route = useRoute()
const { t, setLocale, locale } = useI18n()
const localeStore = useLocaleStore()
const switchLocalePath = useSwitchLocalePath()
const { setUserLocale } = localeStore
const userStore = useUserStore()
const { logout, hasRole, stopImpersonation } = userStore
const { isLoggedIn, isImpersonating, canManageSchedule, canManageAlarm, canManageMaintenance, canManageOperations, user } = storeToRefs(userStore)
const toast = useToast()

const leaveImpersonation = async () => {
  try {
    await stopImpersonation()
    await navigateTo('/users/list')
  } catch {
    // Without a valid session to return to, the administrator has to log in again.
    toast.add({ title: t('users.stopImpersonationError'), icon: 'i-lucide-bomb', color: 'error' })
    await logout()
    await navigateTo('/login')
  }
}

const menu = computed(() => {
  if (!isLoggedIn.value) {
    return []
  }
  const items: any[] = [{
    label: t('nav.home'),
    icon: 'i-lucide-house',
    to: '/'
  }]
  const schedules: any[] = []
  if (canManageSchedule.value) {
    schedules.push(
      { label: t('nav.participants.list'), icon: 'i-lucide-users', to: '/participants/list' },
      { label: t('nav.participants.add'), icon: 'i-lucide-user-plus', to: '/participants/add' },
      { label: t('nav.participants.import'), icon: 'i-lucide-upload', to: '/participants/import' },
      { label: t('nav.service.list'), icon: 'i-lucide-briefcase', to: '/service/list' },
      { label: t('nav.service.add'), icon: 'i-lucide-plus', to: '/service/add' },
      { label: t('nav.plan.list'), icon: 'i-lucide-calendar-days', to: '/plan/list' },
      { label: t('nav.plan.add'), icon: 'i-lucide-calendar-plus', to: '/plan/add' }
    )
  } else if (hasRole('ROLE_USER')) {
    schedules.push({ label: t('nav.plan.list'), icon: 'i-lucide-calendar-days', to: '/plan/list' })
  }
  if (schedules.length) {
    schedules.push({ label: t('nav.plan.mine'), icon: 'i-lucide-calendar-check', to: '/plan/mine' })
  }
  if (schedules.length) {
    items.push({
      label: t('nav.schedules'),
      icon: 'i-lucide-calendar-days',
      defaultOpen: true,
      children: schedules
    })
  }
  // Every signed in user works on maintenance tasks and defects, counts the inventory and writes the winch book;
  // the templates and the winch book settings are for maintenance administrators.
  items.push({
    label: t('nav.maintenance.menu'),
    icon: 'i-lucide-wrench',
    defaultOpen: true,
    children: [
      { label: t('nav.maintenance.overview'), icon: 'i-lucide-plane', to: '/maintenance' },
      { label: t('nav.maintenance.defects'), icon: 'i-lucide-triangle-alert', to: '/maintenance/defects' },
      { label: t('nav.maintenance.schedule'), icon: 'i-lucide-chart-gantt', to: '/maintenance/schedule' },
      { label: t('nav.maintenance.inventory'), icon: 'i-lucide-package', to: '/maintenance/inventory' },
      { label: t('nav.maintenance.winch'), icon: 'i-lucide-truck', to: '/maintenance/winch' },
      ...(canManageMaintenance.value ? [
        { label: t('nav.maintenance.templates'), icon: 'i-lucide-copy', to: '/maintenance/templates' },
        { label: t('nav.maintenance.winchSettings'), icon: 'i-lucide-sliders-horizontal', to: '/maintenance/winch/settings' }
      ] : [])
    ]
  })
  // Every signed in user fills in the start/end of operations checklists, operations administrators search the archive.
  items.push({
    label: t('nav.operations.menu'),
    icon: 'i-lucide-clipboard-check',
    defaultOpen: true,
    children: [
      { label: t('nav.operations.checklists'), icon: 'i-lucide-clipboard-list', to: '/operations' },
      ...(canManageOperations.value ? [{ label: t('nav.operations.archive'), icon: 'i-lucide-archive', to: '/operations/archive' }] : [])
    ]
  })
  if (canManageAlarm.value) {
    items.push({
      label: t('nav.alarms.menu'),
      icon: 'i-lucide-bell',
      defaultOpen: true,
      children: [
        { label: t('nav.alarms.list'), icon: 'i-lucide-list', to: '/alarm/list' }
      ]
    })
  }
  if (hasRole('ROLE_ADMIN')) {
    items.push({ label: t('nav.users'), icon: 'i-lucide-user-cog', to: '/users/list', iconOnly: true })
  }
  items.push({ label: t('nav.requests'), icon: 'i-lucide-bug', to: '/request/list', iconOnly: true })
  if (hasRole('ROLE_ADMIN')) {
    items.push({ label: t('nav.settings'), icon: 'i-lucide-settings', to: '/settings', iconOnly: true })
  }
  return items
})

// In the header bar some entries show only their icon; the label stays for screen readers and as hover title.
// The slideover menu on small screens keeps all labels.
const barMenu = computed(() => menu.value.map(({ iconOnly, ...item }) => iconOnly
  ? { ...item, title: item.label, ui: { linkLabel: 'sr-only' } }
  : item))

// The username opens the personal settings (kiosk PIN).
const account = computed(() => [
  [{ label: user.value?.username, description: t('nav.profile'), icon: 'i-lucide-user-cog', to: '/profile', 'data-profile-link': true }],
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
