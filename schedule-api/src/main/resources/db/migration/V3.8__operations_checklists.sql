-- Checklists for the start (START) and end (END) of operations of an aircraft or other equipment. Maintenance and
-- operations administrators maintain one template per aircraft and kind; members fill in one checklist per aircraft,
-- kind and day, which copies the items of the template. Completed checklists are approved by operations administrators.

CREATE TABLE operations_checklist_template (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    aircraft_id character varying(36) NOT NULL,
    kind character varying(10) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT operations_checklist_template_pkey PRIMARY KEY (id),
    CONSTRAINT operations_checklist_template_kind CHECK (kind IN ('START', 'END')),
    CONSTRAINT operations_checklist_template_unique UNIQUE (aircraft_id, kind),
    CONSTRAINT fk_operations_checklist_template_aircraft FOREIGN KEY (aircraft_id) REFERENCES maintenance_aircraft (id) ON DELETE CASCADE
);

CREATE TABLE operations_checklist_template_item (
    template_id character varying(36) NOT NULL,
    position int4 NOT NULL,
    text character varying(500) NOT NULL,
    CONSTRAINT operations_checklist_template_item_pkey PRIMARY KEY (template_id, position),
    CONSTRAINT fk_operations_checklist_template_item_template FOREIGN KEY (template_id) REFERENCES operations_checklist_template (id) ON DELETE CASCADE
);

-- The checklist of one day. Its items are a copy of the template, so changing or deleting the template keeps the record.
CREATE TABLE operations_checklist (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    aircraft_id character varying(36) NOT NULL,
    kind character varying(10) NOT NULL,
    checklist_date date NOT NULL,
    status character varying(20) NOT NULL,
    created_by_id character varying(36),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    submitted_by_id character varying(36),
    submitted_at timestamp with time zone,
    approved_by_id character varying(36),
    approved_at timestamp with time zone,
    CONSTRAINT operations_checklist_pkey PRIMARY KEY (id),
    CONSTRAINT operations_checklist_kind CHECK (kind IN ('START', 'END')),
    CONSTRAINT operations_checklist_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'APPROVED')),
    CONSTRAINT operations_checklist_unique UNIQUE (aircraft_id, kind, checklist_date),
    CONSTRAINT fk_operations_checklist_aircraft FOREIGN KEY (aircraft_id) REFERENCES maintenance_aircraft (id) ON DELETE CASCADE,
    CONSTRAINT fk_operations_checklist_created_by FOREIGN KEY (created_by_id) REFERENCES auth_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_operations_checklist_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES auth_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_operations_checklist_approved_by FOREIGN KEY (approved_by_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_operations_checklist_date ON operations_checklist (checklist_date);
CREATE INDEX idx_operations_checklist_status ON operations_checklist (status);

CREATE TABLE operations_checklist_item (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    checklist_id character varying(36) NOT NULL,
    position int4 NOT NULL,
    text character varying(500) NOT NULL,
    result character varying(10),
    comment character varying(1000),
    checked_by_id character varying(36),
    checked_at timestamp with time zone,
    defect_id character varying(36),
    CONSTRAINT operations_checklist_item_pkey PRIMARY KEY (id),
    CONSTRAINT operations_checklist_item_result CHECK (result IN ('OK', 'NOT_OK')),
    CONSTRAINT operations_checklist_item_unique UNIQUE (checklist_id, position),
    CONSTRAINT fk_operations_checklist_item_checklist FOREIGN KEY (checklist_id) REFERENCES operations_checklist (id) ON DELETE CASCADE,
    CONSTRAINT fk_operations_checklist_item_checked_by FOREIGN KEY (checked_by_id) REFERENCES auth_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_operations_checklist_item_defect FOREIGN KEY (defect_id) REFERENCES maintenance_defect (id) ON DELETE SET NULL
);

CREATE INDEX idx_operations_checklist_item_defect ON operations_checklist_item (defect_id);

-- Operations administrators are notified when a checklist is submitted.
ALTER TABLE user_notification ADD COLUMN checklist_id character varying(36);
ALTER TABLE user_notification ADD CONSTRAINT fk_user_notification_checklist FOREIGN KEY (checklist_id) REFERENCES operations_checklist (id) ON DELETE CASCADE;
ALTER TABLE user_notification DROP CONSTRAINT user_notification_type;
ALTER TABLE user_notification ADD CONSTRAINT user_notification_type CHECK (type IN ('DEFECT_ASSIGNED', 'CHECKLIST_SUBMITTED'));
CREATE INDEX idx_user_notification_checklist ON user_notification (checklist_id);
