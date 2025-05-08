ALTER TABLE date_participant_availability DROP COLUMN requested;
ALTER TABLE date_participant_availability_aud DROP COLUMN requested;

ALTER TABLE date_participant_availability ADD COLUMN requested_service_id character varying(36);
ALTER TABLE date_participant_availability_aud ADD COLUMN requested_service_id character varying(36);

ALTER TABLE date_participant_availability ADD CONSTRAINT fk_date_participant_availability_requested_service FOREIGN KEY (requested_service_id) REFERENCES service(id);
ALTER TABLE date_participant_availability_aud ADD CONSTRAINT fk_date_participant_availability_aud_requested_service FOREIGN KEY (requested_service_id) REFERENCES service(id);