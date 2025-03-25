import type { Service } from './service';
import type { Participant } from './participant';

type PlanEntry = {
    id: string;
    date: Date;
    startTime: Date;
    endTime: Date;
    service: Service;
    participant: Participant;
}

export type { PlanEntry }