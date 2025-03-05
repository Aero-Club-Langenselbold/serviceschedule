export default defineNuxtRouteMiddleware(() => {
    if (import.meta.server) {
        return
    }
    const userStore = useUserStore()
    const { validate } = userStore
    const { isLoggedIn } = storeToRefs(userStore)

    validate()

    // redirect the user to the login screen if they're not authenticated
    if (!isLoggedIn.value) {
        return navigateTo('/login')
    }
})