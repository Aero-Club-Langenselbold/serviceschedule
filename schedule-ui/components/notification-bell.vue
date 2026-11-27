<template>
  <UPopover v-model:open="open" :content="{ align: 'end', sideOffset: 8 }">
    <UButton color="neutral" variant="ghost" :aria-label="`${$t('notifications.open')} (${$t('notifications.unread', unreadCount)})`" data-notification-bell>
      <UChip :show="unreadCount > 0" :text="unreadCount > 9 ? '9+' : unreadCount" color="error" size="3xl" inset>
        <UIcon name="i-lucide-bell" class="size-5" />
      </UChip>
    </UButton>

    <template #content>
      <div class="w-80 max-w-[calc(100vw-2rem)]" data-notification-list>
        <div class="flex items-center justify-between gap-2 border-b border-default px-3 py-2">
          <p class="text-sm font-semibold text-highlighted">{{ $t('notifications.title') }}</p>
          <UButton
            v-if="unreadCount" color="neutral" variant="link" size="xs" icon="i-lucide-check-check"
            :label="$t('notifications.markAllRead')" data-mark-all-read @click="markAllRead"
          />
        </div>
        <p v-if="!items.length" class="px-3 py-6 text-center text-sm text-muted">{{ $t('notifications.empty') }}</p>
        <ul v-else class="max-h-96 divide-y divide-default overflow-y-auto">
          <li v-for="item in items" :key="item.id">
            <button
              type="button" :data-notification="item.id"
              class="flex w-full gap-2.5 px-3 py-2.5 text-left transition-colors hover:bg-elevated focus-visible:bg-elevated focus-visible:outline-none"
              @click="select(item)"
            >
              <span class="mt-1.5 size-2 shrink-0 rounded-full" :class="item.read ? 'bg-transparent' : 'bg-primary'" />
              <span class="min-w-0 flex-1 space-y-0.5">
                <span class="block text-sm" :class="item.read ? 'text-muted' : 'font-medium text-highlighted'">
                  {{ $t(`notifications.${item.type}`, { actor: item.actor?.name ?? $t('notifications.someone') }) }}
                </span>
                <span v-if="item.defect" class="block truncate text-sm text-default">{{ item.defect.aircraftLabel }} · {{ item.defect.title }}</span>
                <span class="block text-xs text-muted">{{ formatWhen(item.createdAt) }}</span>
              </span>
            </button>
          </li>
        </ul>
      </div>
    </template>
  </UPopover>
</template>

<script lang="ts" setup>
import type { Notifications, UserNotification } from '~/types/notification'

/** Notifications of the signed in user next to their name; a click marks one read and jumps to what it is about. */
const REFRESH_MS = 60_000

const route = useRoute()
const { formatWhen } = useMaintenance()

const open = ref(false)
const unreadCount = ref(0)
const items = ref<UserNotification[]>([])

const fetchNotifications = async () => {
  const { data, error } = await useAPI<Notifications>('/notifications', { method: 'GET' })
  if (!error.value && data.value) {
    unreadCount.value = data.value.unreadCount
    items.value = data.value.items
  }
}

const markRead = (item: UserNotification) => {
  if (!item.read) {
    item.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    return useAPI(`/notifications/${item.id}/read`, { method: 'PUT' })
  }
}

const select = async (item: UserNotification) => {
  open.value = false
  await markRead(item)
  if (item.defect) {
    await navigateTo({ path: '/maintenance/defects', query: { defect: item.defect.id } })
  }
}

const markAllRead = async () => {
  items.value.forEach((item) => { item.read = true })
  unreadCount.value = 0
  await useAPI('/notifications/read-all', { method: 'PUT' })
}

// fresh when opened, on every page change and once a minute
watch(open, (isOpen) => {
  if (isOpen) {
    fetchNotifications()
  }
})
watch(() => route.fullPath, fetchNotifications)

let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  fetchNotifications()
  timer = setInterval(fetchNotifications, REFRESH_MS)
})
onBeforeUnmount(() => clearInterval(timer))
</script>
