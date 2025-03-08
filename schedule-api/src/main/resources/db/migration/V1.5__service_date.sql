CREATE TABLE service_date (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    date DATE NOT NULL,
    service_id character varying(36) NOT NULL,
    plan_id character varying(36) NOT NULL,
    mandatory_assignees INT NOT NULL DEFAULT 1,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL
);

ALTER TABLE service_date ADD CONSTRAINT service_date_pkey PRIMARY KEY (id);
ALTER TABLE service_date ADD CONSTRAINT fk_service_date_service_id FOREIGN KEY (service_id) REFERENCES service(id);
ALTER TABLE service_date ADD CONSTRAINT fk_service_date_plan_id FOREIGN KEY (plan_id) REFERENCES service(id);

-- Create service_date_aud table for auditing
CREATE TABLE service_date_aud (
    id character varying(36) NOT NULL,
	rev int4 NOT NULL,
	revtype int2,
    date DATE,
    service_id character varying(36),
    plan_id character varying(36),
    mandatory_assignees INT,
    start_time TIMESTAMP,
    end_time TIMESTAMP
);


ALTER TABLE service_date_aud ADD CONSTRAINT fk_service_date_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev);