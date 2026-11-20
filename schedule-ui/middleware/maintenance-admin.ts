export default defineNuxtRouteMiddleware(() => {

    const userStore = useUserStore()
    const { validate } = userStore
    const { isLoggedIn, user, canManageMaintenance } = storeToRefs(userStore)

    validate()

    // redirect the user to the permission denied screen if logged in but has neither a matching role nor the right
    if (isLoggedIn.value && user.value && !canManageMaintenance.value) {
        return navigateTo('/permission-denied')
    }
})
