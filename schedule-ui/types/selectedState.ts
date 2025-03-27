import type { Country } from './country';
import type { State } from './state';

type SelectedState = {
    id: string;
    country: Country;
    state: State;
}

export type { SelectedState }