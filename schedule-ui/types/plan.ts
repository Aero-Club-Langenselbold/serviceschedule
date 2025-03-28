import type { PlanEntry } from './planEntry';
import type { ServiceDate } from './serviceDate';

type Plan = {
    id: string;
    name: string;
    description: string;
    dateFrom: Date | null;
    dateUntil: Date | null;
    published: boolean;
    entries: PlanEntry[];
    serviceDates: ServiceDate[];
}

export type { Plan }