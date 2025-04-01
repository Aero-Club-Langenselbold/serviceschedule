import type { Service } from './service';

type Participant = {
    id: string;
    firstname: string;
    lastname: string;
    fullname: string;
    email: string;
    dateOfBirth: Date | undefined;
    status: 'ACTIVE' | 'INACTIVE';
    registrationKey: string;
    services: Service[] | undefined;
}

export type { Participant };
