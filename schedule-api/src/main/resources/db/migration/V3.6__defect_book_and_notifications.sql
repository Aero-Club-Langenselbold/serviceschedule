-- Defect book: defects are worked on (in progress, done, rejected), assigned to users, commented and finally signed off
-- by maintenance administrators. Users are notified when a defect is assigned to them.

ALTER TABLE maintenance_defect DROP CONSTRAINT maintenance_defect_status;
ALTER TABLE maintenance_defect ADD CONSTRAINT maintenance_defect_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE', 'REJECTED'));

-- done_by_id/done_at now record who closed the defect (DONE or REJECTED).
ALTER TABLE maintenance_defect ADD COLUMN signed_by_id character varying(36);
ALTER TABLE maintenance_defect ADD COLUMN signed_at timestamp with time zone;
ALTER TABLE maintenance_defect ADD CONSTRAINT fk_maintenance_defect_signed_by FOREIGN KEY (signed_by_id) REFERENCES auth_user (id) ON DELETE SET NULL;

CREATE INDEX idx_maintenance_defect_status ON maintenance_defect (status);

CREATE TABLE maintenance_defect_assignee (
    defect_id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL,
    CONSTRAINT maintenance_defect_assignee_pkey PRIMARY KEY (defect_id, user_id),
    CONSTRAINT fk_maintenance_defect_assignee_defect FOREIGN KEY (defect_id) REFERENCES maintenance_defect (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_defect_assignee_user FOREIGN KEY (user_id) REFERENCES auth_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_maintenance_defect_assignee_user ON maintenance_defect_assignee (user_id);

-- Append-only activity of a defect: comments, status changes, assignments, sign-off.
CREATE TABLE maintenance_defect_event (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    defect_id character varying(36) NOT NULL,
    action character varying(30) NOT NULL,
    content text,
    actor_id character varying(36),
    -- the user an assignment is about
    subject_id character varying(36),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT maintenance_defect_event_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_defect_event_action CHECK (action IN ('REPORTED', 'UPDATED', 'COMMENTED', 'STARTED', 'DONE', 'REJECTED', 'REOPENED',
        'ASSIGNED', 'UNASSIGNED', 'SIGNED', 'UNSIGNED')),
    CONSTRAINT fk_maintenance_defect_event_defect FOREIGN KEY (defect_id) REFERENCES maintenance_defect (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_defect_event_actor FOREIGN KEY (actor_id) REFERENCES auth_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_maintenance_defect_event_subject FOREIGN KEY (subject_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_maintenance_defect_event_defect ON maintenance_defect_event (defect_id);

CREATE TABLE user_notification (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    user_id character varying(36) NOT NULL,
    type character varying(30) NOT NULL,
    defect_id character varying(36),
    actor_id character varying(36),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    read_at timestamp with time zone,
    CONSTRAINT user_notification_pkey PRIMARY KEY (id),
    CONSTRAINT user_notification_type CHECK (type IN ('DEFECT_ASSIGNED')),
    CONSTRAINT fk_user_notification_user FOREIGN KEY (user_id) REFERENCES auth_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_notification_defect FOREIGN KEY (defect_id) REFERENCES maintenance_defect (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_notification_actor FOREIGN KEY (actor_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_user_notification_user ON user_notification (user_id, created_at);
CREATE INDEX idx_user_notification_defect ON user_notification (defect_id);
