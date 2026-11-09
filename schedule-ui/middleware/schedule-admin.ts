export default defineNuxtRouteMiddleware((to, from) => {

    const userStore = useUserStore()
    const { validate } = userStore
    const { isLoggedIn, user, canManageSchedule } = storeToRefs(userStore)

    validate()

    // redirect the user to the permission denied screen if logged in but has neither a matching role nor the right
    if (isLoggedIn.value && user.value && !canManageSchedule.value) {
        return navigateTo('/permission-denied')
    }
})