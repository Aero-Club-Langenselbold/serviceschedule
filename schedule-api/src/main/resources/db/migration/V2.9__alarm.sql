CREATE TABLE alarm (
    id              character varying(36)  NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    alarm_timestamp timestamp              NOT NULL,
    rfid_id         character varying(32),
    internal_time   character varying(10),
    raw_data        character varying(255),
    status          integer,
    CONSTRAINT alarm_pkey             PRIMARY KEY (id),
    CONSTRAINT alarm_timestamp_unique UNIQUE (alarm_timestamp)
);
