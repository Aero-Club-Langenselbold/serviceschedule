import type { UseFetchOptions } from 'nuxt/app'

export function useAPI<T>(
  url: string | (() => string),
  options?: UseFetchOptions<T>,
) {

  const api = useNuxtApp().$api as (url: string, opts?: any) => Promise<T>
  return api(typeof url === 'function' ? url() : url, options)
    .then(response => {
      return { data: { value: response ?? null }, error: {} }
    })
    .catch(error => {
      return { data: {}, error: { value: error } }
    })
}