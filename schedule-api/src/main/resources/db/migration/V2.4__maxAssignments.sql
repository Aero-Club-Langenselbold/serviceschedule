-- Add registration_key column to participant table
ALTER TABLE participant ADD COLUMN max_assignments int DEFAULT 0;

-- Add registration_key column to participant_aud table
ALTER TABLE participant_aud ADD COLUMN max_assignments int DEFAULT 0;