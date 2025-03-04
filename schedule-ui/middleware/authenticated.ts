export default defineNuxtRouteMiddleware(() => {
    const sessionStore = useUserSessionStore()

    // redirect the user to the login screen if they're not authenticated
    if (!sessionStore.loggedIn) {
        return navigateTo('/login')
    }
})