import { beforeEach, describe, expect, it } from 'vitest'

describe('Features Store', () => {
    const featureStore = useFeatureStore()

    beforeEach(() => {
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
})
