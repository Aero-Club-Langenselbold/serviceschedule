import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({ fail: false }))

mockNuxtImport('useAPI', () => async () => api.fail
    ? { data: {}, error: { value: new Error('blocked') } }
    : { data: { value: [{ id: 'testFeature', description: 'Test Feature', active: true }] }, error: {} })

describe('Features Store', () => {
    let featureStore: ReturnType<typeof useFeatureStore>

    beforeEach(() => {
        featureStore = useFeatureStore()
    })

    beforeEach(() => {
        api.fail = false
        featureStore.features = []
        localStorage.removeItem('features')
    })

    it('starts with empty features', () => {
        expect(featureStore.features).toHaveLength(0)
    })

    it('fetchFeatures loads features from API', async () => {
        await featureStore.fetchFeatures()
        expect(featureStore.features).not.toHaveLength(0)
        expect(featureStore.features[0].id).toBe('testFeature')
    })

    it('fetchFeatures saves features to localStorage', async () => {
        await featureStore.fetchFeatures()
        const stored = localStorage.getItem('features')
        expect(stored).not.toBeNull()
        const parsed = JSON.parse(stored!)
        expect(parsed[0].id).toBe('testFeature')
    })

    it('validateFeatures restores features from localStorage', async () => {
        const mockFeatures = [{ id: 'storedFeature', description: 'Stored', active: true }]
        localStorage.setItem('features', JSON.stringify(mockFeatures))

        await featureStore.validateFeatures()
        expect(featureStore.features[0].id).toBe('storedFeature')
    })

    it('validateFeatures leaves features empty if nothing in localStorage', async () => {
        await featureStore.validateFeatures()
        expect(featureStore.features).toHaveLength(0)
    })

    it('fetchFeatures keeps the last known features if the request fails', async () => {
        const stored = JSON.stringify([{ id: 'storedFeature', description: 'Stored', active: true }])
        localStorage.setItem('features', stored)
        api.fail = true

        await featureStore.fetchFeatures()
        await featureStore.validateFeatures()

        expect(localStorage.getItem('features')).toBe(stored)
        expect(featureStore.features[0].id).toBe('storedFeature')
    })

    it.each(['undefined', 'null', '{broken'])('validateFeatures discards the invalid stored value %s', async (stored) => {
        localStorage.setItem('features', stored)

        await expect(featureStore.validateFeatures()).resolves.toBeUndefined()

        expect(featureStore.features).toHaveLength(0)
        expect(localStorage.getItem('features')).toBeNull()
    })
})
