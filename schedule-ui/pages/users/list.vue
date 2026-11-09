<template>
  <AppPage :title="$t('users.title')" :description="$t('users.description')" :error-message="errorMessage" width="full">
    <UInput v-model="filterValue" icon="i-lucide-search" class="mb-4 w-full sm:max-w-sm" :placeholder="$t('users.filter')" :aria-label="$t('users.filter')" @update:model-value="fetchDataDebounced" />
    <DataTable :data="listData?.content" :columns="columns" />
    <div class="mt-4 flex justify-center">
      <UPagination v-model:page="page" :sibling-count="1" :items-per-page="pageSize" :total="listData?.totalElements" @update:page="fetchData" />
    </div>
  </AppPage>
</template>

<script lang="ts" setup>
import type { TableColumn } from '@nuxt/ui'
import type { Page } from '~/types/page'
import type { UserOverview } from '~/types/user'

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UCheckbox = resolveComponent('UCheckbox')

const { t } = useI18n()
const toast = useToast()
const userStore = useUserStore()
const { impersonate } = userStore
const { user } = storeToRefs(userStore)

const errorMessage = ref<string | null>(null)
const page = ref(1)
const pageSize = ref(20)
const listData = ref<Page<UserOverview> | null>(null)
const availablePermissions = ref<string[]>([])
const filterValue = ref('')
const savingUsers = reactive<Record<string, boolean>>({})

// Rights that a role grants anyway. Roles are managed in the database, so these cannot be changed here.
const permissionsOfRole: Record<string, string[]> = {
  ROLE_SCHEDULE_ADMIN: ['PERMISSION_SCHEDULE_MANAGE'],
  ROLE_ALARM_ADMIN: ['PERMISSION_ALARM_MANAGE']
}

const isImpliedByRole = (row: UserOverview, permission: string) =>
  row.roles.some(role => role === 'ROLE_ADMIN' || permissionsOfRole[role]?.includes(permission))

const isImpersonable = (row: UserOverview) => !row.roles.includes('ROLE_ADMIN') && row.username !== user.value?.username

const togglePermission = async (row: UserOverview, permission: string, granted: boolean) => {
  const permissions = granted
    ? [...row.permissions, permission]
    : row.permissions.filter(existing => existing !== permission)

  savingUsers[row.id] = true
  const { data, error } = await useAPI<UserOverview>(`/user/${row.id}/permissions`, { method: 'PUT', body: permissions })
  savingUsers[row.id] = false

  if (error.value) {
    toast.add({ title: t('users.permissionError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  row.permissions = (data.value as UserOverview).permissions
  toast.add({ title: t('users.permissionSuccess'), icon: 'i-lucide-circle-check', color: 'primary' })
}

const startImpersonation = async (row: UserOverview) => {
  try {
    await impersonate(row.id)
    await navigateTo('/')
  } catch {
    toast.add({ title: t('users.impersonateError'), icon: 'i-lucide-bomb', color: 'error' })
  }
}

const columns = computed<TableColumn<UserOverview>[]>(() => [
  {
    accessorKey: 'username',
    header: t('users.username'),
    meta: { cardTitle: true, class: { td: 'font-medium text-highlighted max-md:break-all' } }
  }, {
    accessorKey: 'fullname',
    header: t('users.fullname'),
    cell: ({ row }) => row.original.fullname ?? t('users.noParticipant')
  }, {
    accessorKey: 'roles',
    header: t('users.roles'),
    cell: ({ row }) => h('div', { class: 'flex flex-wrap gap-1' }, row.original.roles.map(role =>
      h(UBadge, { key: role, variant: 'subtle', color: 'neutral' }, () => t(`users.roleNames.${role}`))))
  }, {
    accessorKey: 'permissions',
    header: t('users.permissions'),
    cell: ({ row }) => h('div', { class: 'flex flex-col gap-2' }, availablePermissions.value.map((permission) => {
      const implied = isImpliedByRole(row.original, permission)
      return h(UCheckbox, {
        key: permission,
        label: t(`users.permissionNames.${permission}`),
        description: implied ? t('users.impliedByRole') : undefined,
        modelValue: implied || row.original.permissions.includes(permission),
        disabled: implied || savingUsers[row.original.id],
        'onUpdate:modelValue': (value: boolean | 'indeterminate') => togglePermission(row.original, permission, value === true)
      })
    }))
  }, {
    id: 'actions',
    cell: ({ row }) => h('div', { class: 'flex justify-end' }, [
      h(UButton, {
        color: 'neutral',
        variant: 'outline',
        icon: 'i-lucide-venetian-mask',
        disabled: !isImpersonable(row.original),
        onClick: () => startImpersonation(row.original)
      }, () => t('users.impersonate'))
    ])
  }
])

const fetchData = async () => {
  errorMessage.value = null
  const search = encodeURIComponent(filterValue.value)
  const { data, error } = await useAPI<Page<UserOverview>>(`/user/page?page=${page.value - 1}&size=${pageSize.value}&search=${search}`, { method: 'GET' })

  if (error.value) {
    errorMessage.value = t('users.loadError')
  } else {
    listData.value = data.value as Page<UserOverview>
  }
}

let debounceTimeout: ReturnType<typeof setTimeout>
const fetchDataDebounced = () => {
  clearTimeout(debounceTimeout)
  debounceTimeout = setTimeout(() => {
    page.value = 1
    fetchData()
  }, 500)
}

const fetchPermissions = async () => {
  const { data } = await useAPI<string[]>('/user/permissions', { method: 'GET' })
  availablePermissions.value = data.value ?? []
}

onMounted(() => {
  fetchPermissions()
  fetchData()
})

definePageMeta({
    middleware: ['authenticated', 'admin']
})
</script>
