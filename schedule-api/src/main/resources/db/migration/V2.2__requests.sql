CREATE TABLE request (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    headline character varying(255) NOT NULL,
    description text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    type character varying(15) DEFAULT 'BUG' NOT NULL,
    status character varying(15) DEFAULT 'OPEN' NOT NULL,
    participant_id character varying(36)
);

ALTER TABLE request ADD CONSTRAINT request_pkey PRIMARY KEY (id);
ALTER TABLE request ADD CONSTRAINT fk_request_author FOREIGN KEY (participant_id) REFERENCES participant(id);