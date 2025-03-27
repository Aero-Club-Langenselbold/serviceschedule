-- Add registration_key column to participant table
ALTER TABLE participant ADD COLUMN registration_key VARCHAR(36);

-- Add registration_key column to participant_aud table
ALTER TABLE participant_aud ADD COLUMN registration_key VARCHAR(36);