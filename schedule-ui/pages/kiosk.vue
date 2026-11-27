<template>
  <AppPage :title="$t('kiosk.title')" :description="$t('kiosk.description', { user: user?.username ?? '' })" width="lg">
    <ul class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" data-kiosk-tiles>
      <li v-for="tile in KIOSK_DESTINATIONS" :key="tile.to">
        <NuxtLink
          :to="tile.to"
          class="flex h-full min-h-32 items-center gap-5 rounded-2xl border border-default bg-default p-5 transition-colors hover:border-primary active:bg-elevated focus-visible:outline-2 focus-visible:outline-primary"
        >
          <span class="flex size-16 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <UIcon :name="tile.icon" class="size-8" />
          </span>
          <span class="min-w-0">
            <span class="block text-lg font-semibold text-highlighted">{{ $t(tile.label) }}</span>
            <span class="mt-1 block text-sm text-muted">{{ $t(tile.hint) }}</span>
          </span>
        </NuxtLink>
      </li>
    </ul>
  </AppPage>
</template>

<script setup lang="ts">
// Start page of the kiosk mode: large tiles for the areas a kiosk session reaches.
definePageMeta({
  middleware: ['authenticated']
})

const { user } = storeToRefs(useUserStore())
</script>
