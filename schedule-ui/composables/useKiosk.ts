// Kiosk mode: members sign in on a shared tablet with e-mail address and PIN. The session is a plain member limited
// to the maintenance and operations pages; the backend enforces the same limits.

export type LoginMode = 'regular' | 'kiosk'

const LOGIN_MODE_KEY = 'loginMode'

/** The kiosk start page with the tiles. */
export const KIOSK_HOME = '/kiosk'

// Pages a kiosk session may open: maintenance and operations as far as plain members reach them.
const KIOSK_PATHS: RegExp[] = [
  /^\/kiosk$/,
  /^\/login$/,
  /^\/permission-denied$/,
  /^\/maintenance$/,
  /^\/maintenance\/(defects|schedule|inventory)$/,
  /^\/operations$/,
  /^\/operations\/checklist\/[^/]+\/[^/]+$/,
  /^\/operations\/checklists\/[^/]+$/
]
// the maintenance board of an aircraft, but not the administration pages next to it
const KIOSK_EXCLUDED = /^\/maintenance\/templates$/

export function isKioskPath(path: string) {
  const normalized = path.length > 1 ? path.replace(/\/+$/, '') : path
  if (KIOSK_EXCLUDED.test(normalized)) {
    return false
  }
  return KIOSK_PATHS.some(pattern => pattern.test(normalized)) || /^\/maintenance\/[^/]+$/.test(normalized)
}

/** The sign in mode chosen on this device (kept in the browser, regular by default). */
export function storedLoginMode(): LoginMode {
  try {
    return import.meta.client && localStorage.getItem(LOGIN_MODE_KEY) === 'kiosk' ? 'kiosk' : 'regular'
  } catch {
    return 'regular'
  }
}

export function storeLoginMode(mode: LoginMode) {
  try {
    if (import.meta.client) {
      localStorage.setItem(LOGIN_MODE_KEY, mode)
    }
  } catch {
    // private mode or blocked storage: the choice is just not remembered
  }
}


/** The areas of the kiosk mode (start page tiles and header navigation); `label`/`hint` are i18n keys. */
export const KIOSK_DESTINATIONS = [
  { to: '/operations', icon: 'i-lucide-clipboard-check', label: 'nav.operations.checklists', hint: 'kiosk.hints.checklists' },
  { to: '/maintenance', icon: 'i-lucide-plane', label: 'nav.maintenance.overview', hint: 'kiosk.hints.maintenance' },
  { to: '/maintenance/defects', icon: 'i-lucide-triangle-alert', label: 'nav.maintenance.defects', hint: 'kiosk.hints.defects' },
  { to: '/maintenance/schedule', icon: 'i-lucide-chart-gantt', label: 'nav.maintenance.schedule', hint: 'kiosk.hints.schedule' },
  { to: '/maintenance/inventory', icon: 'i-lucide-package', label: 'nav.maintenance.inventory', hint: 'kiosk.hints.inventory' }
] as const

/** The destination a path belongs to: the longest matching one (an aircraft board belongs to the maintenance overview). */
export function activeKioskDestination(path: string) {
  return [...KIOSK_DESTINATIONS]
    .filter(item => path === item.to || path.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0]?.to ?? null
}
