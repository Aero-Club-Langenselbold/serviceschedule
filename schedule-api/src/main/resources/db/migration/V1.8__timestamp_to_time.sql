ALTER TABLE service_date ALTER COLUMN start_time TYPE TIME;
ALTER TABLE service_date ALTER COLUMN end_time TYPE TIME;

-- Alter the start_time and end_time columns from TIMESTAMP to TIME in service_date_aud table
ALTER TABLE service_date_aud ALTER COLUMN start_time TYPE TIME;
ALTER TABLE service_date_aud ALTER COLUMN end_time TYPE TIME;