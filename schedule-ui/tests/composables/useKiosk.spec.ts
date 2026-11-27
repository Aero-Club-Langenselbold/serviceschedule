import { afterEach, describe, expect, it } from 'vitest'
import { activeKioskDestination, isKioskPath, storedLoginMode, storeLoginMode } from '~/composables/useKiosk'

describe('isKioskPath', () => {
  it.each([
    '/kiosk', '/login', '/maintenance', '/maintenance/', '/maintenance/defects', '/maintenance/schedule', '/maintenance/inventory',
    '/maintenance/a1', '/operations', '/operations/checklist/a1/start', '/operations/checklists/c1'
  ])('opens %s', path => expect(isKioskPath(path)).toBe(true))

  it.each([
    '/', '/profile', '/request/list', '/settings', '/users/list', '/plan/mine', '/plan/list', '/participants/list', '/alarm/list',
    '/maintenance/templates', '/operations/archive', '/operations/print/a1/START'
  ])('closes %s', path => expect(isKioskPath(path)).toBe(false))
})

describe('activeKioskDestination', () => {
  it('picks the most specific area', () => {
    expect(activeKioskDestination('/maintenance')).toBe('/maintenance')
    expect(activeKioskDestination('/maintenance/a1')).toBe('/maintenance')
    expect(activeKioskDestination('/maintenance/defects')).toBe('/maintenance/defects')
    expect(activeKioskDestination('/operations/checklist/a1/start')).toBe('/operations')
    expect(activeKioskDestination('/kiosk')).toBeNull()
  })
})

describe('login mode', () => {
  afterEach(() => localStorage.removeItem('loginMode'))

  it('is regular until kiosk is chosen on the device', () => {
    expect(storedLoginMode()).toBe('regular')
    storeLoginMode('kiosk')
    expect(storedLoginMode()).toBe('kiosk')
    storeLoginMode('regular')
    expect(storedLoginMode()).toBe('regular')
  })
})

