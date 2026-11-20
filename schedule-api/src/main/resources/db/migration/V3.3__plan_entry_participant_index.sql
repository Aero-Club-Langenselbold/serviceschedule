-- "My services" looks up the plan entries of one participant.
CREATE INDEX idx_plan_entry_participant ON plan_entry (participant_id);
CREATE INDEX idx_plan_entry_plan ON plan_entry (plan_id);
