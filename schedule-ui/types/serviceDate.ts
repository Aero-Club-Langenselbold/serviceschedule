import type { Plan } from './plan';
import type { Service } from './service';

type ServiceDate = {
    id: string;
    date: Date;
    mandatoryAssignees: number;
    startTime: Date;
    endTime: Date;
    service: Service;
    plan: Plan;
}

export type { ServiceDate }