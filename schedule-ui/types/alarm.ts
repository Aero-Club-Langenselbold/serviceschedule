type Alarm = {
    id: string
    timestamp: Date
    rfidId: string
    internalTime: string
    rawData: string
    status: 'DISARMED' | 'ARMED' | 'UNCHANGED' | 'NOT_IMPLEMENTED'
}

export type { Alarm }

