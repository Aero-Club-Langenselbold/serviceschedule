type Alarm = {
    id: string
    timestamp: string
    rfidId: string
    internalTime: string
    rawData: string
    status: number
}

type AlarmDeviceStatus = {
    id: string
    timestamp: string
    betrieb: number
    scharf: number
    unscharf: number
    sabotage: number
    stoerung: number
    alarm: number
    zone1: number
    zone2: number
    zone3: number
    zone4: number
    zone5: number
    zone6: number
    zone7: number
    zone8: number
}

export type { Alarm, AlarmDeviceStatus }

