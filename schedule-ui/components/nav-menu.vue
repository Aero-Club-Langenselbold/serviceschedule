<template>
  <UContainer>
    <!--<UHorizontalNavigation :links="links" class="border-b border-gray-200 dark:border-gray-800" />-->
  </UContainer>
  <div>
    <button @click="setLocale('en')">en</button>
    <button @click="setLocale('de')">de</button>
  </div>
</template>

<script lang="ts" setup>
const links = [{
  label: 'Profile',
  avatar: {
    src: 'https://avatars.githubusercontent.com/u/739984?v=4'
  },
  badge: 100
}, {
  label: 'Installation',
  icon: 'i-heroicons-home',
  to: '/getting-started/installation'
}, {
  label: 'Horizontal Navigation',
  icon: 'i-heroicons-chart-bar',
  to: '/components/horizontal-navigation'
}, {
  label: 'Command Palette',
  icon: 'i-heroicons-command-line',
  to: '/components/command-palette'
}]
const { setLocale } = useI18n()
</script>