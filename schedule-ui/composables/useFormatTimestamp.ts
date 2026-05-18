export const useFormatTimestamp = () => {
  const formatUtcTimestamp = (utcString: string | null | undefined): string => {
    if (!utcString) return ''
    const date = new Date(utcString.endsWith('Z') ? utcString : utcString + 'Z')
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }

  return { formatUtcTimestamp }
}
