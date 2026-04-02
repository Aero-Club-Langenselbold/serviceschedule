import { beforeEach, describe, expect, it } from 'vitest'

describe('Locale Store', () => {
    const localeStore = useLocaleStore()

    beforeEach(() => {
        localStorage.removeItem('locale')
    })

    it('defaults to de locale', async () => {
        await localeStore.validate()
        expect(localeStore.getUserLocale()).toBe('de')
    })

    it('setUserLocale changes the locale', async () => {
        await localeStore.setUserLocale('en')
        expect(localeStore.getUserLocale()).toBe('en')
    })

    it('setUserLocale persists locale to localStorage', async () => {
        await localeStore.setUserLocale('en')
        expect(localStorage.getItem('locale')).toBe('en')
    })

    it('validate restores locale from localStorage', async () => {
        localStorage.setItem('locale', 'en')
        await localeStore.validate()
        expect(localeStore.getUserLocale()).toBe('en')
    })

    it('validate falls back to de when localStorage is empty', async () => {
        await localeStore.validate()
        expect(localeStore.getUserLocale()).toBe('de')
    })
})
