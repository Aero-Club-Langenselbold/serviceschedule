import type { UseFetchOptions } from 'nuxt/app'

export function useAPI<T>(
  url: string | (() => string),
  options?: UseFetchOptions<T>,
) {
  console.log('useAPI called with URL:', url)
  return useFetch(url, {
    ...options,
    $fetch: useNuxtApp().$api
  })
}