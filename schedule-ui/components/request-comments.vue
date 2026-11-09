<template>
  <section class="mt-3 space-y-3 whitespace-normal md:mx-2">
    <h3 class="text-sm font-semibold text-highlighted">{{ $t('request.comments.title') }}</h3>
    <p v-if="loadFailed" class="text-sm text-error">{{ $t('request.comments.loadError') }}</p>
    <p v-else-if="!comments.length" class="text-sm text-muted">{{ $t('request.comments.empty') }}</p>
    <ul v-else class="space-y-2">
      <li v-for="comment in comments" :key="comment.id" class="rounded-lg border border-default p-3 text-sm">
        <p class="mb-1 text-xs text-muted">{{ comment.author ?? $t('request.comments.unknownAuthor') }} · {{ formatDate(comment.createdAt) }}</p>
        <p class="whitespace-pre-line text-default">{{ comment.content }}</p>
      </li>
    </ul>
    <form v-if="canComment" class="space-y-2" @submit.prevent="addComment">
      <UTextarea v-model="draft" :rows="2" autoresize class="w-full" :maxlength="5000" :placeholder="$t('request.comments.placeholder')" :aria-label="$t('request.comments.label')" />
      <div class="flex justify-end">
        <UButton type="submit" icon="i-lucide-send" :loading="sending" :disabled="!draft.trim()" :label="$t('request.comments.submit')" />
      </div>
    </form>
    <p v-else class="text-sm text-muted">{{ $t('request.comments.closed') }}</p>
  </section>
</template>

<script lang="ts" setup>
import type { Request } from '~/types/request'
import type { RequestComment } from '~/types/requestComment'

const props = defineProps<{
  requestId: string
  status: Request['status']
}>()

const { t, locale } = useI18n()
const toast = useToast()

const comments = ref<RequestComment[]>([])
const loadFailed = ref(false)
const draft = ref('')
const sending = ref(false)

const canComment = computed(() => props.status === 'OPEN' || props.status === 'IN_PROGRESS')

const formatDate = (date: string) => new Date(date).toLocaleString(locale.value, {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
})

const fetchComments = async () => {
  const { data, error } = await useAPI<RequestComment[]>(`/request/${props.requestId}/comments`, { method: 'GET' })
  loadFailed.value = !!error.value
  comments.value = data.value ?? []
}

const addComment = async () => {
  sending.value = true
  const { data, error } = await useAPI<RequestComment>(`/request/${props.requestId}/comments`, { method: 'POST', body: { content: draft.value } })
  sending.value = false

  if (error.value) {
    toast.add({ title: t('request.comments.error'), icon: 'i-lucide-bomb', color: 'error' })
    return
  }
  comments.value.push(data.value as RequestComment)
  draft.value = ''
}

onMounted(fetchComments)
</script>
