-- Add unique constraint to participant.email
ALTER TABLE participant
ADD CONSTRAINT unique_email UNIQUE (email);

-- Add unique constraint to auth_user.username
ALTER TABLE auth_user
ADD CONSTRAINT unique_username UNIQUE (username);