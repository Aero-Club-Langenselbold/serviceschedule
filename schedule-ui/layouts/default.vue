<template>
  <div class="flex min-h-dvh flex-col">
    <KioskHeader v-if="isKiosk" />
    <NavMenu v-else />
    <main class="flex-1">
      <slot />
    </main>
    <Footer />
  </div>
</template>

<script lang="ts" setup>
// The kiosk mode (shared tablet) has its own header and larger touch targets, see assets/css/main.css.
const { isKiosk } = storeToRefs(useUserStore())

useHead({
  htmlAttrs: {
    'data-kiosk': computed(() => isKiosk.value ? 'true' : undefined)
  }
})
</script>
