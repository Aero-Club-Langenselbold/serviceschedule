<template>
  <UNavigationMenu orientation="horizontal" content-orientation="vertical" :items="items" class="data-[orientation=horizontal]:border-b border-(--ui-border) data-[orientation=horizontal]:w-full data-[orientation=vertical]:w-48" />
  <pre>{{ {user} }}</pre>
</template>

<script lang="ts" setup>
const { t, setLocale } = useI18n()
const userStore = useUserStore()
const { logout, user } = userStore
const { isLoggedIn } = storeToRefs(userStore)

const menu = computed(() => {
  const items = []
  if (isLoggedIn.value) {
    items.push({
      label: computed(() => t('nav.home')),
      icon: 'i-heroicons-home',
      to: '/'
    })
    items.push({
      label: computed(() => t('nav.participants.menu')),
      icon: 'i-lucide-users',
      defaultOpen: true,
      children: [
        {
          label: computed(() => t('nav.participants.list')),
          icon: 'i-lucide-list',
          to: '/participants'
        },
        {
          label: computed(() => t('nav.participants.add')),
          icon: 'i-lucide-user-plus',
          to: '/participants/add'
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
          to: '/service'
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
          to: '/plan'
        },
        {
          label: computed(() => t('nav.plan.add')),
          icon: 'i-lucide-plus',
          to: '/plan/add'
        }
      ]
    })
  }
  return items;
})
const sessionInfo = computed(() => {
  const items = []
  if (isLoggedIn.value) {
    items.push({
      label: user?.username,
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
    items.push({
      icon: 'i-lucide-settings'
    })
  }
  items.push({
    icon: 'circle-flags:de',
    onSelect: () => setLocale('de')
  })
  items.push({
    icon: 'circle-flags:us',
    onSelect: () => setLocale('en')
  })
  return items;
})
const items = ref([
  menu.value,
  sessionInfo.value,
])

</script>