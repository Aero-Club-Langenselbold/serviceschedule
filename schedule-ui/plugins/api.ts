export default defineNuxtPlugin((nuxtApp) => {
    const userStore = useUserStore()
    const { session } = storeToRefs(userStore)
    const config = useRuntimeConfig()

    const api = $fetch.create({
        baseURL: config.public.API_URL,
        onRequest({ request, options, error }) {
            if (session?.value?.token) {
                options.headers.set('Authorization', `Bearer ${session.value?.token}`)
            }
        },
        async onResponseError({ response }) {
            if (response.status === 401) {
                await nuxtApp.runWithContext(() => navigateTo('/login'))
            }
        }
    })

    // Expose to useNuxtApp().$api
    return {
        provide: {
            api
        }
    }
})