import type { PlanEntry } from './planEntry';

type Plan = {
    id: string;
    name: string;
    description: string;
    dateFrom: Date;
    dateUntil: Date;
    published: boolean;
    entries: PlanEntry[];
}

export type { Plan }