/** Longest side of uploaded photos; larger ones are scaled down in the browser before the upload. */
export const PHOTO_MAX_SIDE = 1600
/** What the backend accepts. */
export const PHOTO_MAX_BYTES = 10 * 1024 * 1024
const PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

/** Page of a day of the winch book. */
export const winchDayPath = (date: string) => `/maintenance/winch/days/${date}`

/**
 * Scales a photo down to PHOTO_MAX_SIDE as JPEG. Files the browser cannot decode (or where scaling does not help)
 * stay as they are; the backend then decides whether it accepts them.
 */
export async function scalePhoto(file: File): Promise<File> {
    try {
        const bitmap = await createImageBitmap(file)
        const scale = Math.min(1, PHOTO_MAX_SIDE / Math.max(bitmap.width, bitmap.height))
        if (scale === 1 && PHOTO_TYPES.includes(file.type) && file.size <= 2 * 1024 * 1024) {
            bitmap.close()
            return file
        }
        const canvas = document.createElement('canvas')
        canvas.width = Math.round(bitmap.width * scale)
        canvas.height = Math.round(bitmap.height * scale)
        canvas.getContext('2d')!.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
        bitmap.close()
        const blob = await new Promise<Blob | null>(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.85))
        if (!blob) {
            return file
        }
        const name = file.name.replace(/\.[^.]*$/, '') || 'foto'
        return new File([blob], `${name}.jpg`, { type: 'image/jpeg' })
    } catch {
        return file
    }
}

/** Whether a file can be uploaded as photo at all. */
export const isAcceptedPhoto = (file: File) => PHOTO_TYPES.includes(file.type) && file.size <= PHOTO_MAX_BYTES
