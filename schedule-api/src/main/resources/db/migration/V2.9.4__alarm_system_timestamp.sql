ALTER TABLE alarm ADD COLUMN system_timestamp TIMESTAMP UNIQUE;
ALTER TABLE alarm DROP CONSTRAINT IF EXISTS alarm_alarm_timestamp_key;
