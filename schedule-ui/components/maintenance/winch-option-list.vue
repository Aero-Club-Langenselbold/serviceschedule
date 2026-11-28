<template>
  <section class="rounded-xl border border-default bg-default p-4" :data-option-list="kind">
    <h2 class="text-lg font-semibold text-highlighted">{{ title }}</h2>
    <p class="mt-1 text-sm text-muted">{{ hint }}</p>

    <ul class="mt-4 space-y-2">
      <li
        v-for="(option, index) in options" :key="option.id" class="flex flex-wrap items-center gap-2 rounded-lg border border-default p-2"
        :class="option.active ? '' : 'bg-elevated'" :data-option="option.id"
      >
        <div class="flex shrink-0 flex-col">
          <UButton
            icon="i-lucide-chevron-up" color="neutral" variant="ghost" size="xs" :disabled="index === 0 || busy"
            :aria-label="$t('winch.settings.moveUp')" data-move-up @click="move(index, -1)"
          />
          <UButton
            icon="i-lucide-chevron-down" color="neutral" variant="ghost" size="xs" :disabled="index === options.length - 1 || busy"
            :aria-label="$t('winch.settings.moveDown')" data-move-down @click="move(index, 1)"
          />
        </div>
        <UInput
          v-model="names[option.id]" class="min-w-40 flex-1" :aria-label="$t('winch.settings.name')" maxlength="100" data-option-name
          @keydown.enter.prevent="save(option)"
        />
        <UBadge v-if="!option.active" color="neutral" variant="subtle" :label="$t('winch.inactive')" data-inactive />
        <div class="flex shrink-0 items-center gap-1">
          <UButton
            v-if="(names[option.id] ?? '').trim() !== option.name" icon="i-lucide-save" :label="$t('common.save')" size="sm" :loading="busy"
            data-save-option @click="save(option)"
          />
          <UButton
            v-if="!option.active" icon="i-lucide-rotate-ccw" color="neutral" variant="outline" size="sm" :label="$t('winch.settings.activate')"
            :disabled="busy" data-activate @click="save(option, true)"
          />
          <ConfirmationDialog
            v-else variant="ghost" color="error" :icon="option.used ? 'i-lucide-archive' : 'i-lucide-trash-2'" :loading="false" :button-label="undefined"
            button-class="" :tooltip="option.used ? $t('winch.settings.deactivate') : $t('common.delete')"
            :modal-title="option.used ? $t('winch.settings.deactivateConfirm', { name: option.name }) : $t('winch.settings.deleteConfirm', { name: option.name })"
            :confirm-label="option.used ? $t('winch.settings.deactivate') : $t('common.delete')" :abort-label="$t('common.cancel')"
            :on-confirm="() => remove(option)"
          />
        </div>
      </li>
    </ul>
    <p v-if="!options.length" class="mt-4 text-sm text-muted">{{ $t('winch.settings.empty') }}</p>

    <form class="mt-4 flex gap-2" data-add-option @submit.prevent="add">
      <UInput v-model="newName" class="flex-1" :placeholder="placeholder" :aria-label="placeholder" maxlength="100" data-new-option />
      <UButton type="submit" icon="i-lucide-plus" :label="$t('winch.settings.add')" :disabled="!newName.trim()" :loading="busy" />
    </form>
  </section>
</template>

<script lang="ts" setup>
import type { WinchOption, WinchOptionKind, WinchSettings } from '~/types/winch'

/** One list of the winch book settings (ropes or incident types): add, rename, reorder, remove or reactivate. */
const props = defineProps<{ kind: WinchOptionKind, options: WinchOption[], title: string, hint: string, placeholder: string }>()
const emit = defineEmits<{ update: [settings: WinchSettings] }>()

const { t } = useI18n()
const toast = useToast()

const busy = ref(false)
const newName = ref('')
const names = reactive<Record<string, string>>({})

watch(() => props.options, (options) => {
  options.forEach((option) => { names[option.id] = option.name })
}, { immediate: true })

const send = async (path: string, method: 'POST' | 'PUT' | 'DELETE', body?: unknown) => {
  busy.value = true
  const { data, error } = await useAPI<WinchSettings>(`/maintenance/winch/settings/${props.kind}${path}`, { method, body })
  busy.value = false
  if (error.value) {
    toast.add({ title: error.value.statusCode === 409 ? t('winch.settings.duplicate') : t('winch.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return false
  }
  emit('update', data.value as WinchSettings)
  return true
}

const add = async () => {
  if (await send('', 'POST', { name: newName.value.trim() })) {
    newName.value = ''
  }
}

const save = (option: WinchOption, activate = false) =>
  send(`/${option.id}`, 'PUT', { name: (names[option.id] ?? '').trim() || option.name, active: activate || option.active })

const remove = (option: WinchOption) => send(`/${option.id}`, 'DELETE')

const move = (index: number, offset: number) => {
  const ids = props.options.map(option => option.id)
  const [moved] = ids.splice(index, 1)
  ids.splice(index + offset, 0, moved!)
  send('/order', 'PUT', { ids })
}
</script>
