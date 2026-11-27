<template>
  <AppPage :title="$t('profile.title')" :description="settings?.username" :error-message="errorMessage" width="sm">
    <section class="rounded-xl border border-default bg-default p-4" data-kiosk-section>
      <div class="flex items-start justify-between gap-3">
        <div>
          <h2 class="font-semibold text-highlighted">{{ $t('profile.kiosk.title') }}</h2>
          <p class="mt-1 text-sm text-muted">{{ $t('profile.kiosk.description') }}</p>
        </div>
        <UBadge
          v-if="settings" :color="settings.kioskPinSet ? 'success' : 'neutral'" variant="subtle" class="shrink-0"
          :label="settings.kioskPinSet ? $t('profile.kiosk.set') : $t('profile.kiosk.notSet')" data-kiosk-state
        />
      </div>

      <form class="mt-5 space-y-4" @submit.prevent="save">
        <UFormField :label="settings?.kioskPinSet ? $t('profile.kiosk.newPin') : $t('profile.kiosk.pin')">
          <UPinInput v-model="pin" :length="6" type="number" mask otp :aria-label="$t('profile.kiosk.pin')" data-pin />
        </UFormField>
        <UFormField :label="$t('profile.kiosk.confirmPin')" :error="mismatch ? $t('profile.kiosk.mismatch') : undefined">
          <UPinInput v-model="confirmation" :length="6" type="number" mask otp :aria-label="$t('profile.kiosk.confirmPin')" data-pin-confirm />
        </UFormField>
        <p class="text-xs text-muted">{{ $t('profile.kiosk.hint') }}</p>
        <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <ConfirmationDialog
            v-if="settings?.kioskPinSet" variant="outline" color="error" icon="i-lucide-trash-2" :loading="false"
            :button-label="$t('profile.kiosk.remove')" button-class="" :tooltip="$t('profile.kiosk.remove')"
            :modal-title="$t('profile.kiosk.removeConfirm')" :confirm-label="$t('common.delete')" :abort-label="$t('common.cancel')"
            :on-confirm="remove"
          />
          <UButton type="submit" icon="i-lucide-save" :label="$t('profile.kiosk.save')" :loading="saving" :disabled="!complete || mismatch" data-save-pin />
        </div>
      </form>
    </section>
  </AppPage>
</template>

<script lang="ts" setup>
type UserSettings = { username: string, kioskPinSet: boolean }

/**
 * Personal settings, opened from the username in the header. The kiosk PIN is stored hashed like a password: it is
 * never shown again, the page only tells whether one is set.
 */
const { t } = useI18n()
const toast = useToast()

const settings = ref<UserSettings | null>(null)
const errorMessage = ref<string | null>(null)
// digits as the number pin input reports them; empty fields are undefined
const pin = ref<(number | undefined)[]>([])
const confirmation = ref<(number | undefined)[]>([])
const saving = ref(false)

const value = (digits: (number | undefined)[]) => digits.map(digit => digit ?? '').join('')
const complete = computed(() => /^\d{6}$/.test(value(pin.value)) && /^\d{6}$/.test(value(confirmation.value)))
const mismatch = computed(() => value(confirmation.value).length === 6 && value(pin.value) !== value(confirmation.value))

const clear = () => {
  pin.value = []
  confirmation.value = []
}

const load = async () => {
  const { data, error } = await useAPI<UserSettings>('/user/me/settings', { method: 'GET' })
  if (error.value) {
    errorMessage.value = t('profile.loadError')
    return
  }
  settings.value = data.value as UserSettings
}

const save = async () => {
  if (!complete.value || mismatch.value) {
    return
  }
  saving.value = true
  const { data, error } = await useAPI<UserSettings>('/user/me/kiosk-pin', { method: 'PUT', body: { pin: value(pin.value) } })
  saving.value = false
  clear()
  if (error.value) {
    toast.add({ title: t('profile.kiosk.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  settings.value = data.value as UserSettings
  toast.add({ title: t('profile.kiosk.saved'), icon: 'i-lucide-circle-check', color: 'primary' })
}

const remove = async () => {
  const { data, error } = await useAPI<UserSettings>('/user/me/kiosk-pin', { method: 'DELETE' })
  if (error.value) {
    toast.add({ title: t('profile.kiosk.saveError'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  settings.value = data.value as UserSettings
  toast.add({ title: t('profile.kiosk.removed'), icon: 'i-lucide-circle-check', color: 'primary' })
}

onMounted(load)

definePageMeta({
  middleware: ['authenticated']
})
</script>
