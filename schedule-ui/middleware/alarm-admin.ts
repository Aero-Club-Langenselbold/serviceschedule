export default defineNuxtRouteMiddleware((to, from) => {

    const userStore = useUserStore()
    const { validate } = userStore
    const { isLoggedIn, user } = storeToRefs(userStore)

    validate()

    // redirect the user to the permission denied screen if logged in but has neither ROLE_ADMIN nor ROLE_ALARM_ADMIN
    if (
        isLoggedIn.value &&
        user.value &&
        !(
            user.value.roles.includes('ROLE_ADMIN') ||
            user.value.roles.includes('ROLE_ALARM_ADMIN')
        )
    ) {
        return navigateTo('/permission-denied')
    }
})