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
const assignableRoles = ref<string[]>([])
const filterValue = ref('')
const savingUsers = reactive<Record<string, boolean>>({})

// Administrators hold every right anyway, so for them the rights cannot be granted or revoked on their own.
const isImpliedByRole = (row: UserOverview, _permission: string) => row.roles.includes('ROLE_ADMIN')

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

// Administrators cannot revoke their own administrator role (the backend refuses it as well).
const isOwnAdminRole = (row: UserOverview, role: string) => role === 'ROLE_ADMIN' && row.username === user.value?.username && row.roles.includes('ROLE_ADMIN')

const toggleRole = async (row: UserOverview, role: string, granted: boolean) => {
  const roles = (granted ? [...row.roles, role] : row.roles.filter(existing => existing !== role))
    .filter(existing => assignableRoles.value.includes(existing))

  savingUsers[row.id] = true
  const { data, error } = await useAPI<UserOverview>(`/user/${row.id}/roles`, { method: 'PUT', body: roles })
  savingUsers[row.id] = false

  if (error.value) {
    toast.add({ title: t('users.roleError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  row.roles = (data.value as UserOverview).roles
  toast.add({ title: t('users.roleSuccess'), icon: 'i-lucide-circle-check', color: 'primary' })
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
    // the base role (ROLE_USER) only as a badge, everything an administrator assigns as a checkbox
    cell: ({ row }) => h('div', { class: 'flex flex-col gap-2' }, [
      ...row.original.roles.filter(role => !assignableRoles.value.includes(role)).map(role =>
        h(UBadge, { key: role, variant: 'subtle', color: 'neutral', class: 'self-start' }, () => t(`users.roleNames.${role}`))),
      ...assignableRoles.value.map((role) => {
        const own = isOwnAdminRole(row.original, role)
        return h(UCheckbox, {
          key: role,
          label: t(`users.roleNames.${role}`),
          description: own ? t('users.ownAdminRole') : undefined,
          modelValue: row.original.roles.includes(role),
          disabled: own || savingUsers[row.original.id],
          'data-role': role,
          'onUpdate:modelValue': (value: boolean | 'indeterminate') => toggleRole(row.original, role, value === true)
        })
      })
    ])
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
        'data-permission': permission,
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
  const [permissions, roles] = await Promise.all([
    useAPI<string[]>('/user/permissions', { method: 'GET' }),
    useAPI<string[]>('/user/roles', { method: 'GET' })
  ])
  availablePermissions.value = permissions.data.value ?? []
  assignableRoles.value = roles.data.value ?? []
}

onMounted(() => {
  fetchPermissions()
  fetchData()
})

definePageMeta({
    middleware: ['authenticated', 'admin']
})
</script>
