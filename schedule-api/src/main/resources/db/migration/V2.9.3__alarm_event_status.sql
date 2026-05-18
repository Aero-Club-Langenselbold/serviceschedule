ALTER TABLE alarm ADD COLUMN armed boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN disarmed boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN sabotage boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN fault boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN alarm boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone1 boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone2 boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone3 boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone4 boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone5 boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone6 boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone7 boolean DEFAULT false NOT NULL;
ALTER TABLE alarm ADD COLUMN zone8 boolean DEFAULT false NOT NULL;

DROP TABLE alarm_device_status;
