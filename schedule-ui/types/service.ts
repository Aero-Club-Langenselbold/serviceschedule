import type { Settings } from './settings';

type Service = {
    id: string;
    servicename: string;
    settings: Settings[];
}

export type { Service }