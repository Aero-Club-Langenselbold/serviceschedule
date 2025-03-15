export default defineNuxtRouteMiddleware((to, from) => {

    const userStore = useUserStore()
    const { validate } = userStore
    const { isLoggedIn, user } = storeToRefs(userStore)

    validate()

    // redirect the user to the permission denied screen if he is logged in but not an admin
    console.log(user.value.roles)
    if (isLoggedIn.value && user.value && !user.value.roles.includes('ROLE_ADMIN')) {
        return navigateTo('/permission-denied')
    }
})
