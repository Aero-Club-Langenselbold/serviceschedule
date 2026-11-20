-- Maintenance module: aircraft, task templates, kanban tasks (with checklist, notes, history) and defects.

-- Role for maintenance administrators. 'ROLE_MAINTENANCE_ADMIN' does not fit into 20 characters.
ALTER TABLE auth_role ALTER COLUMN name TYPE character varying(30);
INSERT INTO auth_role (id, name) VALUES ('00000000-0000-0000-0000-000000000005', 'ROLE_MAINTENANCE_ADMIN');

CREATE TABLE maintenance_aircraft (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    aircraft_type character varying(100) NOT NULL,
    registration character varying(20) NOT NULL,
    workshop_start date,
    workshop_end date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT maintenance_aircraft_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_aircraft_registration_unique UNIQUE (registration),
    CONSTRAINT maintenance_aircraft_workshop_period CHECK (workshop_start IS NULL OR workshop_end IS NULL OR workshop_end >= workshop_start)
);

CREATE TABLE maintenance_template (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    skill_level character varying(20) NOT NULL,
    duration_minutes int4,
    CONSTRAINT maintenance_template_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_template_skill_level CHECK (skill_level IN ('BEGINNER', 'EXPERIENCED', 'WORKSHOP_LEAD')),
    CONSTRAINT maintenance_template_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0)
);

-- Aircraft types a template applies to; no row means the template applies to every aircraft.
CREATE TABLE maintenance_template_aircraft_type (
    template_id character varying(36) NOT NULL,
    aircraft_type character varying(100) NOT NULL,
    CONSTRAINT maintenance_template_aircraft_type_pkey PRIMARY KEY (template_id, aircraft_type),
    CONSTRAINT fk_maintenance_template_aircraft_type_template FOREIGN KEY (template_id) REFERENCES maintenance_template (id) ON DELETE CASCADE
);

CREATE TABLE maintenance_template_checklist_item (
    template_id character varying(36) NOT NULL,
    position int4 NOT NULL,
    text character varying(500) NOT NULL,
    CONSTRAINT maintenance_template_checklist_item_pkey PRIMARY KEY (template_id, position),
    CONSTRAINT fk_maintenance_template_checklist_item_template FOREIGN KEY (template_id) REFERENCES maintenance_template (id) ON DELETE CASCADE
);

CREATE TABLE maintenance_defect (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    aircraft_id character varying(36) NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    priority character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    reported_by_id character varying(36),
    reported_at timestamp with time zone DEFAULT now() NOT NULL,
    done_by_id character varying(36),
    done_at timestamp with time zone,
    CONSTRAINT maintenance_defect_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_defect_priority CHECK (priority IN ('VERY_LOW', 'LOW', 'NORMAL', 'HIGH')),
    CONSTRAINT maintenance_defect_status CHECK (status IN ('OPEN', 'DONE')),
    CONSTRAINT fk_maintenance_defect_aircraft FOREIGN KEY (aircraft_id) REFERENCES maintenance_aircraft (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_defect_reported_by FOREIGN KEY (reported_by_id) REFERENCES auth_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_maintenance_defect_done_by FOREIGN KEY (done_by_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_maintenance_defect_aircraft ON maintenance_defect (aircraft_id);

CREATE TABLE maintenance_task (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    aircraft_id character varying(36) NOT NULL,
    template_id character varying(36),
    defect_id character varying(36),
    title character varying(255) NOT NULL,
    description text,
    skill_level character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    duration_minutes int4,
    deadline date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    done_by_id character varying(36),
    done_at timestamp with time zone,
    signed_by_id character varying(36),
    signed_at timestamp with time zone,
    CONSTRAINT maintenance_task_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_task_defect_unique UNIQUE (defect_id),
    CONSTRAINT maintenance_task_skill_level CHECK (skill_level IN ('BEGINNER', 'EXPERIENCED', 'WORKSHOP_LEAD')),
    CONSTRAINT maintenance_task_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT maintenance_task_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    -- only finished tasks can be signed off
    CONSTRAINT maintenance_task_signed_when_done CHECK (signed_at IS NULL OR status = 'DONE'),
    CONSTRAINT fk_maintenance_task_aircraft FOREIGN KEY (aircraft_id) REFERENCES maintenance_aircraft (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_task_template FOREIGN KEY (template_id) REFERENCES maintenance_template (id) ON DELETE SET NULL,
    CONSTRAINT fk_maintenance_task_defect FOREIGN KEY (defect_id) REFERENCES maintenance_defect (id) ON DELETE SET NULL,
    CONSTRAINT fk_maintenance_task_done_by FOREIGN KEY (done_by_id) REFERENCES auth_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_maintenance_task_signed_by FOREIGN KEY (signed_by_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_maintenance_task_aircraft ON maintenance_task (aircraft_id);
CREATE INDEX idx_maintenance_task_template ON maintenance_task (template_id);

CREATE TABLE maintenance_task_checklist_item (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    task_id character varying(36) NOT NULL,
    position int4 NOT NULL,
    text character varying(500) NOT NULL,
    done boolean DEFAULT false NOT NULL,
    done_by_id character varying(36),
    done_at timestamp with time zone,
    CONSTRAINT maintenance_task_checklist_item_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_task_checklist_item_position_unique UNIQUE (task_id, position),
    CONSTRAINT fk_maintenance_task_checklist_item_task FOREIGN KEY (task_id) REFERENCES maintenance_task (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_task_checklist_item_done_by FOREIGN KEY (done_by_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE TABLE maintenance_task_note (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    task_id character varying(36) NOT NULL,
    author_id character varying(36),
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT maintenance_task_note_pkey PRIMARY KEY (id),
    CONSTRAINT fk_maintenance_task_note_task FOREIGN KEY (task_id) REFERENCES maintenance_task (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_task_note_author FOREIGN KEY (author_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_maintenance_task_note_task ON maintenance_task_note (task_id);

-- Append-only log of what happened to a task; the UI translates the action.
CREATE TABLE maintenance_task_history (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    task_id character varying(36) NOT NULL,
    actor_id character varying(36),
    action character varying(30) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT maintenance_task_history_pkey PRIMARY KEY (id),
    CONSTRAINT fk_maintenance_task_history_task FOREIGN KEY (task_id) REFERENCES maintenance_task (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_task_history_actor FOREIGN KEY (actor_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_maintenance_task_history_task ON maintenance_task_history (task_id);
