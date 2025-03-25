import type { Service } from './service';

type Participant = {
    id: string;
    firstname: string;
    lastname: string;
    email: string;
    dateOfBirth: Date;
    status: 'ACTIVE' | 'INACTIVE';
    services: Service[];
}

export type { Participant }