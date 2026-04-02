import { describe, expect, it } from 'vitest'

describe('useDateRangeItems', () => {
    it('returns exactly 12 items', () => {
        const items = useDateRangeItems()
        expect(items.value).toHaveLength(12)
    })

    it('every item has a label and value', () => {
        const items = useDateRangeItems()
        for (const item of items.value) {
            expect(item.label).toBeTruthy()
            expect(item.value).toBeTruthy()
        }
    })

    it('first item (January) has value 01-01', () => {
        const items = useDateRangeItems()
        expect(items.value[0].value).toBe('01-01')
    })

    it('last item (December) has value 12-01', () => {
        const items = useDateRangeItems()
        expect(items.value[11].value).toBe('12-01')
    })

    it('values are formatted as MM-01', () => {
        const items = useDateRangeItems()
        for (const item of items.value) {
            expect(item.value).toMatch(/^\d{2}-01$/)
        }
    })

    it('values are in month-ascending order', () => {
        const items = useDateRangeItems()
        const months = items.value.map(i => parseInt(i.value.split('-')[0]))
        for (let i = 1; i < months.length; i++) {
            expect(months[i]).toBe(months[i - 1] + 1)
        }
    })
})
