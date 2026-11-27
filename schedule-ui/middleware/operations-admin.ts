export default defineNuxtRouteMiddleware(() => {

    const userStore = useUserStore()
    const { validate } = userStore
    const { isLoggedIn, user, canManageOperations } = storeToRefs(userStore)

    validate()

    // the archive of the checklists is for operations administrators ("Betriebsadmin") and administrators
    if (isLoggedIn.value && user.value && !canManageOperations.value) {
        return navigateTo('/permission-denied')
    }
})
