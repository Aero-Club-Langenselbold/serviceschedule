CREATE TABLE common_settings (
	id character varying(36) NOT NULL,
	version int4 DEFAULT 0 NOT NULL,
	setting_key varchar(100) NOT NULL UNIQUE,
	setting_value varchar(255) NOT NULL
);

ALTER TABLE common_settings ADD CONSTRAINT common_settings_pkey PRIMARY KEY (id);

INSERT INTO common_settings (id, version, setting_key, setting_value) VALUES ('550e8400-e29b-41d4-a716-446655440000', 0, 'min_participation_count', '4');