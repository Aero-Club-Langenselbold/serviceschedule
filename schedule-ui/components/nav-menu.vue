<template>
  <UNavigationMenu orientation="horizontal" :items="items" class="data-[orientation=horizontal]:border-b border-(--ui-border) data-[orientation=horizontal]:w-full data-[orientation=vertical]:w-48" />
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
  }
  return items;
})
const guide = {
  label: 'Guide',
  icon: 'i-lucide-book-open',
  children: [
    {
      label: 'Introduction',
      description: 'Fully styled and customizable components for Nuxt.',
      icon: 'i-lucide-house'
    },
    {
      label: 'Installation',
      description: 'Learn how to install and configure Nuxt UI in your application.',
      icon: 'i-lucide-cloud-download'
    },
    {
      label: 'Icons',
      icon: 'i-lucide-smile',
      description: 'You have nothing to do, @nuxt/icon will handle it automatically.'
    },
    {
      label: 'Colors',
      icon: 'i-lucide-swatch-book',
      description: 'Choose a primary and a neutral color from your Tailwind CSS theme.'
    },
    {
      label: 'Theme',
      icon: 'i-lucide-cog',
      description:
        'You can customize components by using the `class` / `ui` props or in your app.config.ts.'
    }
  ]
}
const composables = {
  label: 'Composables',
  icon: 'i-lucide-database',
  children: [
    {
      label: 'defineShortcuts',
      icon: 'i-lucide-file-text',
      description: 'Define shortcuts for your application.',
      to: '/composables/define-shortcuts'
    },
    {
      label: 'useOverlay',
      icon: 'i-lucide-file-text',
      description: 'Display a modal/slideover within your application.',
      to: '/composables/use-overlay'
    },
    {
      label: 'useToast',
      icon: 'i-lucide-file-text',
      description: 'Display a toast within your application.',
      to: '/composables/use-toast'
    }
  ]
}
const components = {
  label: 'Components',
  icon: 'i-lucide-box',
  to: '/components',
  active: true,
  defaultOpen: true,
  children: [
    {
      label: 'Link',
      icon: 'i-lucide-file-text',
      description: 'Use NuxtLink with superpowers.',
      to: '/components/link'
    },
    {
      label: 'Modal',
      icon: 'i-lucide-file-text',
      description: 'Display a modal within your application.',
      to: '/components/modal'
    },
    {
      label: 'NavigationMenu',
      icon: 'i-lucide-file-text',
      description: 'Display a list of links.',
      to: '/components/navigation-menu'
    },
    {
      label: 'Pagination',
      icon: 'i-lucide-file-text',
      description: 'Display a list of pages.',
      to: '/components/pagination'
    },
    {
      label: 'Popover',
      icon: 'i-lucide-file-text',
      description: 'Display a non-modal dialog that floats around a trigger element.',
      to: '/components/popover'
    },
    {
      label: 'Progress',
      icon: 'i-lucide-file-text',
      description: 'Show a horizontal bar to indicate task progression.',
      to: '/components/progress'
    }
  ]
}
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