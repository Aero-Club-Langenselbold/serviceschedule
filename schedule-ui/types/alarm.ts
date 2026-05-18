type Alarm = {
    id: string
    timestamp: string
    rfidId: string
    internalTime: string
    rawData: string
    status: number
    armed: boolean
    disarmed: boolean
    sabotage: boolean
    fault: boolean
    alarm: boolean
    zone1: boolean
    zone2: boolean
    zone3: boolean
    zone4: boolean
    zone5: boolean
    zone6: boolean
    zone7: boolean
    zone8: boolean
}

export type { Alarm }

