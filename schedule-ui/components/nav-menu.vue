<template>
  <UNavigationMenu orientation="horizontal" content-orientation="vertical" :items="items" class="data-[orientation=horizontal]:border-b border-(--ui-border) data-[orientation=horizontal]:w-full data-[orientation=vertical]:w-48" />
</template>

<script lang="ts" setup>

const { t, setLocale } = useI18n()
const localeStore = useLocaleStore()
const { setUserLocale } = localeStore
const userStore = useUserStore()
const { logout } = userStore
const { isLoggedIn, user } = storeToRefs(userStore)

const menu = computed(() => {
  const items = []
  if (isLoggedIn.value) {
    items.push({
      label: computed(() => t('nav.home')),
      icon: 'i-heroicons-home',
      to: '/'
    })
  }
  if (isLoggedIn.value && user.value?.roles.includes('ROLE_ADMIN')) {
    items.push({
      label: computed(() => t('nav.participants.menu')),
      icon: 'i-lucide-users',
      defaultOpen: true,
      children: [
        {
          label: computed(() => t('nav.participants.list')),
          icon: 'i-lucide-list',
          to: '/participants/list'
        },
        {
          label: computed(() => t('nav.participants.add')),
          icon: 'i-lucide-user-plus',
          to: '/participants/add'
        },
        {
          label: computed(() => t('nav.participants.import')),
          icon: 'i-lucide-upload',
          to: '/participants/import'
        }
      ]
    })
    items.push({
      label: computed(() => t('nav.service.menu')),
      icon: 'i-lucide-package',
      children: [
        {
          label: computed(() => t('nav.service.list')),
          icon: 'i-lucide-list',
          to: '/service/list'
        },
        {
          label: computed(() => t('nav.service.add')),
          icon: 'i-lucide-package-plus',
          to: '/service/add'
        }
      ]
    })
    items.push({
      label: computed(() => t('nav.plan.menu')),
      icon: 'i-lucide-tablet-smartphone',
      children: [
        {
          label: computed(() => t('nav.plan.list')),
          icon: 'i-lucide-list',
          to: '/plan/list'
        },
        {
          label: computed(() => t('nav.plan.add')),
          icon: 'i-lucide-plus',
          to: '/plan/add'
        }
      ]
    })
  } else if (isLoggedIn.value && user.value?.roles.includes('ROLE_USER')) {
    items.push({
      label: computed(() => t('nav.plan.usermenu')),
      icon: 'i-lucide-tablet-smartphone',
      to: '/plan/list'
    })
  }
  return items;
})
const sessionInfo = computed(() => {
  const items = []
  if (isLoggedIn.value) {
    items.push({
      label: user.value?.username,
      icon: 'i-lucide-user'
    })
    items.push({
      label: 'Logout',
      icon: 'i-lucide-log-out',
      onSelect: async () => {
        await logout()
        navigateTo('/login')
      }
    })
    if (user.value?.roles.includes('ROLE_ADMIN')) {
      items.push({
        icon: 'i-lucide-settings',
        onSelect: () => navigateTo('/settings')
      })
    }
  } else {
    items.push({
      label: 'Login',
      icon: 'i-lucide-log-in',
      onSelect: async () => {
        navigateTo('/login')
      }
    })
  }
  items.push({
    icon: 'circle-flags:de',
    onSelect: () => {
      setUserLocale('de')
      setLocale('de')
    }
  })
  items.push({
    icon: 'circle-flags:us',
    onSelect: () => {
      setUserLocale('en')
      setLocale('en')
    }
  })
  return items;
})
const items = ref([
  menu.value,
  sessionInfo.value,
])

</script>