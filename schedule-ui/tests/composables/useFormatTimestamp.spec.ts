import { describe, expect, it } from 'vitest'

describe('useFormatTimestamp', () => {
    const { formatUtcTimestamp } = useFormatTimestamp()

    it('returns empty string for null', () => {
        expect(formatUtcTimestamp(null)).toBe('')
    })

    it('returns empty string for undefined', () => {
        expect(formatUtcTimestamp(undefined)).toBe('')
    })

    it('returns empty string for empty string', () => {
        expect(formatUtcTimestamp('')).toBe('')
    })

    it('formats a timestamp string matching dd.MM.yyyy HH:mm:ss pattern', () => {
        const result = formatUtcTimestamp('2026-05-07T22:38:45')
        expect(result).toMatch(/^\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}$/)
    })

    it('treats timestamps without Z suffix as UTC', () => {
        const withoutZ = formatUtcTimestamp('2026-05-07T22:38:45')
        const withZ = formatUtcTimestamp('2026-05-07T22:38:45Z')
        expect(withoutZ).toBe(withZ)
    })

    it('formats zero-padded day and month correctly', () => {
        const result = formatUtcTimestamp('2026-01-03T00:00:00Z')
        // day is 03 and month is 01 – at minimum they must be two digits
        expect(result).toMatch(/^\d{2}\.\d{2}\.\d{4}/)
    })

    it('year appears in the correct position of the output', () => {
        const result = formatUtcTimestamp('2026-05-07T22:38:45Z')
        const parts = result.split(' ')
        const dateParts = parts[0].split('.')
        expect(dateParts[2]).toBe('2026')
    })
})
