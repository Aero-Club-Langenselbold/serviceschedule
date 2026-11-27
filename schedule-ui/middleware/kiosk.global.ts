// A kiosk session (e-mail + PIN on a shared tablet) only opens the maintenance and operations pages a plain member
// reaches; everything else leads to the kiosk start page. The kiosk start page is only for kiosk sessions.
export default defineNuxtRouteMiddleware((to) => {
    if (import.meta.server) {
        return
    }
    const userStore = useUserStore()
    userStore.validate()
    const { isKiosk } = storeToRefs(userStore)

    if (isKiosk.value && !isKioskPath(to.path)) {
        return navigateTo(KIOSK_HOME)
    }
    if (!isKiosk.value && to.path === KIOSK_HOME) {
        return navigateTo('/')
    }
})
