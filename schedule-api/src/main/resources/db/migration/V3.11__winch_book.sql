-- Winch book ("Windenbuch") of the club's winch: maintenance administrators define the ropes of the winch and the kinds
-- of incidents that can be recorded. Winch drivers record incidents per rope for the current day, upload photos per rope
-- and write remarks for the day and per rope. Ropes and incident types that are referenced are only deactivated.

CREATE TABLE winch_rope (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    name character varying(100) NOT NULL,
    position int4 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    CONSTRAINT winch_rope_pkey PRIMARY KEY (id),
    CONSTRAINT winch_rope_name_unique UNIQUE (name)
);

CREATE TABLE winch_incident_type (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    name character varying(100) NOT NULL,
    position int4 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    CONSTRAINT winch_incident_type_pkey PRIMARY KEY (id),
    CONSTRAINT winch_incident_type_name_unique UNIQUE (name)
);

-- Defaults: two ropes and the usual incidents.
INSERT INTO winch_rope (id, version, name, position, active) VALUES
    ('5b0d8f0e-6f3a-4c1e-9a51-000000000001', 0, 'Seil 1', 0, true),
    ('5b0d8f0e-6f3a-4c1e-9a51-000000000002', 0, 'Seil 2', 1, true);

INSERT INTO winch_incident_type (id, version, name, position, active) VALUES
    ('7c1e9a2f-0b4d-4e2a-8c63-000000000001', 0, 'Seilriss', 0, true),
    ('7c1e9a2f-0b4d-4e2a-8c63-000000000002', 0, 'Sollbruchstellenriss', 1, true),
    ('7c1e9a2f-0b4d-4e2a-8c63-000000000003', 0, 'Vorseilriss', 2, true),
    ('7c1e9a2f-0b4d-4e2a-8c63-000000000004', 0, 'Schlaufe', 3, true),
    ('7c1e9a2f-0b4d-4e2a-8c63-000000000005', 0, 'Sonstiges', 4, true);

-- One page of the book per day, created with the first entry of that day; it holds the general remark of the day.
CREATE TABLE winch_day (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    day_date date NOT NULL,
    remark character varying(4000),
    remark_updated_by_id character varying(36),
    remark_updated_at timestamp with time zone,
    CONSTRAINT winch_day_pkey PRIMARY KEY (id),
    CONSTRAINT winch_day_date_unique UNIQUE (day_date),
    CONSTRAINT fk_winch_day_remark_updated_by FOREIGN KEY (remark_updated_by_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

-- Everybody who wrote something into the page of a day on that day is a winch driver of the day.
CREATE TABLE winch_day_driver (
    day_id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL,
    CONSTRAINT winch_day_driver_pkey PRIMARY KEY (day_id, user_id),
    CONSTRAINT fk_winch_day_driver_day FOREIGN KEY (day_id) REFERENCES winch_day (id) ON DELETE CASCADE,
    CONSTRAINT fk_winch_day_driver_user FOREIGN KEY (user_id) REFERENCES auth_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_winch_day_driver_user ON winch_day_driver (user_id);

-- The remark of a rope on a day.
CREATE TABLE winch_rope_day (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    day_id character varying(36) NOT NULL,
    rope_id character varying(36) NOT NULL,
    remark character varying(4000),
    updated_by_id character varying(36),
    updated_at timestamp with time zone,
    CONSTRAINT winch_rope_day_pkey PRIMARY KEY (id),
    CONSTRAINT winch_rope_day_unique UNIQUE (day_id, rope_id),
    CONSTRAINT fk_winch_rope_day_day FOREIGN KEY (day_id) REFERENCES winch_day (id) ON DELETE CASCADE,
    CONSTRAINT fk_winch_rope_day_rope FOREIGN KEY (rope_id) REFERENCES winch_rope (id) ON DELETE RESTRICT,
    CONSTRAINT fk_winch_rope_day_updated_by FOREIGN KEY (updated_by_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_winch_rope_day_rope ON winch_rope_day (rope_id);

CREATE TABLE winch_incident (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    day_id character varying(36) NOT NULL,
    rope_id character varying(36) NOT NULL,
    type_id character varying(36) NOT NULL,
    driver_id character varying(36),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    comment character varying(1000),
    CONSTRAINT winch_incident_pkey PRIMARY KEY (id),
    CONSTRAINT fk_winch_incident_day FOREIGN KEY (day_id) REFERENCES winch_day (id) ON DELETE CASCADE,
    CONSTRAINT fk_winch_incident_rope FOREIGN KEY (rope_id) REFERENCES winch_rope (id) ON DELETE RESTRICT,
    CONSTRAINT fk_winch_incident_type FOREIGN KEY (type_id) REFERENCES winch_incident_type (id) ON DELETE RESTRICT,
    CONSTRAINT fk_winch_incident_driver FOREIGN KEY (driver_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_winch_incident_day ON winch_incident (day_id);
CREATE INDEX idx_winch_incident_rope ON winch_incident (rope_id);
CREATE INDEX idx_winch_incident_type ON winch_incident (type_id);

-- Photos of a rope on a day; the image itself is kept in its own table so listing photos does not load it.
CREATE TABLE winch_photo (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    day_id character varying(36) NOT NULL,
    rope_id character varying(36) NOT NULL,
    uploaded_by_id character varying(36),
    uploaded_at timestamp with time zone DEFAULT now() NOT NULL,
    filename character varying(255),
    content_type character varying(50) NOT NULL,
    size_bytes int8 NOT NULL,
    CONSTRAINT winch_photo_pkey PRIMARY KEY (id),
    CONSTRAINT winch_photo_content_type CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp', 'image/gif')),
    CONSTRAINT fk_winch_photo_day FOREIGN KEY (day_id) REFERENCES winch_day (id) ON DELETE CASCADE,
    CONSTRAINT fk_winch_photo_rope FOREIGN KEY (rope_id) REFERENCES winch_rope (id) ON DELETE RESTRICT,
    CONSTRAINT fk_winch_photo_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_winch_photo_day ON winch_photo (day_id);
CREATE INDEX idx_winch_photo_rope ON winch_photo (rope_id);

CREATE TABLE winch_photo_content (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    photo_id character varying(36) NOT NULL,
    data bytea NOT NULL,
    CONSTRAINT winch_photo_content_pkey PRIMARY KEY (id),
    CONSTRAINT winch_photo_content_photo_unique UNIQUE (photo_id),
    CONSTRAINT fk_winch_photo_content_photo FOREIGN KEY (photo_id) REFERENCES winch_photo (id) ON DELETE CASCADE
);
