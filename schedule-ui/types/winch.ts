import type { Person } from '~/types/maintenance'

/** A rope or a kind of incident, as maintained in the settings; used = referenced by entries (removing only deactivates it). */
export type WinchOption = {
    id: string
    name: string
    position: number
    active: boolean
    used: boolean
}

export type WinchSettings = {
    ropes: WinchOption[]
    incidentTypes: WinchOption[]
}

/** The two lists of the settings, as named in the API path. */
export type WinchOptionKind = 'ropes' | 'incident-types'

export type WinchOptionRef = {
    id: string
    name: string
    active: boolean
}

/** text null without remark; version is sent back when changing it. */
export type WinchRemark = {
    text: string | null
    version: number | null
    updatedBy: Person | null
    updatedAt: string | null
}

export type WinchIncident = {
    id: string
    type: WinchOptionRef
    driver: Person | null
    createdAt: string
    comment: string | null
    deletable: boolean
}

export type WinchPhoto = {
    id: string
    filename: string | null
    contentType: string
    size: number
    uploadedBy: Person | null
    uploadedAt: string
    deletable: boolean
}

export type WinchRopeEntries = {
    rope: WinchOptionRef
    remark: WinchRemark
    incidents: WinchIncident[]
    photos: WinchPhoto[]
}

/** The page of a day. editable: incidents and photos can be added (today); remarksEditable: remarks can be changed. */
export type WinchDay = {
    date: string
    today: boolean
    exists: boolean
    remark: WinchRemark
    drivers: Person[]
    ropes: WinchRopeEntries[]
    incidentTypes: WinchOptionRef[]
    editable: boolean
    remarksEditable: boolean
}

export type WinchTypeCount = {
    typeId: string
    name: string
    count: number
}

export type WinchDaySummary = {
    date: string
    drivers: Person[]
    incidents: WinchTypeCount[]
    incidentCount: number
    photoCount: number
    hasRemarks: boolean
}
