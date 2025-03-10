ALTER TABLE plan ADD COLUMN published BOOLEAN DEFAULT FALSE;
ALTER TABLE plan_aud ADD COLUMN published BOOLEAN;

-- Create service_settings table
CREATE TABLE service_settings (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    range_from character varying(5) NOT NULL,
    range_to character varying(5) NOT NULL,
    mandatory_assignees INT,
    saturdays BOOLEAN,
    sundays BOOLEAN,
    public_holidays BOOLEAN,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    service_id character varying(36) NOT NULL
);

-- Add constraints to service_settings table
ALTER TABLE service_settings ADD CONSTRAINT service_settings_pkey PRIMARY KEY (id);
ALTER TABLE service_settings ADD CONSTRAINT fk_service_settings_service FOREIGN KEY (service_id) REFERENCES service(id);

-- Create service_settings_aud table for auditing
CREATE TABLE service_settings_aud (
    id character varying NOT NULL,
    rev int4 NOT NULL,
    revtype int2,
    range_from VARCHAR(5),
    range_to VARCHAR(5),
    mandatory_assignees INT,
    saturdays BOOLEAN,
    sundays BOOLEAN,
    public_holidays BOOLEAN,
    start_time TIME,
    end_time TIME,
    service_id character varying(36) NOT NULL
);

-- Add constraints to service_settings_aud table
ALTER TABLE service_settings_aud ADD CONSTRAINT fk_service_settings_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev);