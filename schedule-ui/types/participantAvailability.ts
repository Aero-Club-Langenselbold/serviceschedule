import type { Plan } from './plan';
import type { Participant } from './participant';

type ParticipantAvailability = {
  id: string;
  date: Date;
  requested: boolean;
  participant: Participant;
  Plan: Plan;
}

export type { ParticipantAvailability };