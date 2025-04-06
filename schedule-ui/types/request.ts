import type { Participant } from './participant';

type Request = {
    id: string;
    headline: string;
    description: string;
    createdAt: Date | undefined;
    type: 'BUG' | 'FEATURE' | undefined;
    status: 'OPEN' | 'IN_PROGRESS' | 'CLOSED' | 'REJECTED';
    author: Participant | undefined | null;
}

export type { Request };

