-- Personal 6-digit PIN for the kiosk mode, stored as a BCrypt hash like the password (never in plain text).
ALTER TABLE auth_user ADD COLUMN kiosk_pin character varying(128);
