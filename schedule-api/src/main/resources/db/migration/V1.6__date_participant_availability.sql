-- Create date_participant_availability table
CREATE TABLE date_participant_availability (
    id character varying(36) NOT NULL,
    date DATE NOT NULL,
    requested BOOLEAN,
    participant_id character varying(36) NOT NULL,
    plan_id character varying(36) NOT NULL
);

ALTER TABLE date_participant_availability ADD CONSTRAINT date_participant_availability_pkey PRIMARY KEY (id);
ALTER TABLE date_participant_availability ADD CONSTRAINT fk_date_participant_availability_participant_id FOREIGN KEY (participant_id) REFERENCES participant(id);
ALTER TABLE date_participant_availability ADD CONSTRAINT fk_date_participant_availability_plan_id FOREIGN KEY (plan_id) REFERENCES plan(id);

-- Create date_participant_availability_aud table for auditing
CREATE TABLE date_participant_availability_aud (
    id character varying(36) NOT NULL,
    rev INT NOT NULL,
    revtype TINYINT,
    date DATE,
    requested BOOLEAN,
    participant_id character varying(36),
    plan_id character varying(36)
);

ALTER TABLE date_participant_availability_aud ADD CONSTRAINT fk_date_participant_availability_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev);