<template>
  <AppPage :title="$t('app.welcome')" :description="`${userData?.firstname ?? ''} ${userData?.lastname ?? ''}`.trim() || undefined" width="md">
    <p class="mb-6 text-muted">{{ displayText }}</p>
    <ul class="grid gap-3 sm:grid-cols-2">
      <li v-for="tile in tiles" :key="tile.to">
        <NuxtLink :to="tile.to" class="flex items-center gap-4 rounded-xl border border-default bg-default p-4 transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary">
          <span class="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <UIcon :name="tile.icon" class="size-5" />
          </span>
          <span class="font-medium text-highlighted">{{ tile.label }}</span>
          <UIcon name="i-lucide-chevron-right" class="ml-auto size-4 shrink-0 text-dimmed" />
        </NuxtLink>
      </li>
    </ul>
  </AppPage>
</template>

<script setup lang="ts">
import type { Participant } from '~/types/participant'

const userStore = useUserStore()
const { user, canManageSchedule, canManageAlarm } = storeToRefs(userStore)
const { hasRole } = userStore
const { t } = useI18n()
const userData = ref<Participant | null>({
  id: '',
  firstname: '',
  lastname: '',
  fullname: '',
  email: '',
  dateOfBirth: new Date(),
  status: 'INACTIVE',
  maxAssignments: 0,
  registrationKey: '',
  participantServices: []
})

definePageMeta({
    middleware: ['authenticated']
})

const tiles = computed(() => [
  ...(canManageSchedule.value ? [
    { label: t('nav.participants.list'), icon: 'i-lucide-users', to: '/participants/list' },
    { label: t('nav.service.list'), icon: 'i-lucide-briefcase', to: '/service/list' }
  ] : []),
  ...(canManageSchedule.value || hasRole('ROLE_USER') ? [
    { label: t('nav.plan.usermenu'), icon: 'i-lucide-calendar-days', to: '/plan/list' }
  ] : []),
  ...(canManageAlarm.value ? [
    { label: t('nav.alarms.list'), icon: 'i-lucide-bell', to: '/alarm/list' }
  ] : []),
  { label: t('nav.requests'), icon: 'i-lucide-bug', to: '/request/list' },
  ...(hasRole('ROLE_ADMIN') ? [
    { label: t('nav.users'), icon: 'i-lucide-user-cog', to: '/users/list' }
  ] : [])
])

// see who we got here
const displayText = ref('')
if (user.value?.roles.includes('ROLE_ADMIN')) {
    displayText.value = t('app.user.adminWelcome')
} else {
    const {data, error } = await useAPI<Participant>('/participant/fetchByMail/' + user.value?.username, { method: 'GET' })

    userData.value = data.value
    if (error.value || data.value === null) {
      displayText.value = t('app.user.userWelcome')
    } else {
      displayText.value = t('app.user.participantWelcome')
    }
}
</script>