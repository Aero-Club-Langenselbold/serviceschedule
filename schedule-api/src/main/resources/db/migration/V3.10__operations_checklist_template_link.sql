-- A checklist remembers the template it was started from. When the template is deleted and created anew, a checklist of
-- today still in progress is outdated and discarded the next time it is opened; if nobody opens it, it stays.
ALTER TABLE operations_checklist ADD COLUMN template_id character varying(36);
ALTER TABLE operations_checklist ADD CONSTRAINT fk_operations_checklist_template FOREIGN KEY (template_id) REFERENCES operations_checklist_template (id) ON DELETE SET NULL;
CREATE INDEX idx_operations_checklist_template ON operations_checklist (template_id);

UPDATE operations_checklist SET template_id = (
    SELECT t.id FROM operations_checklist_template t WHERE t.aircraft_id = operations_checklist.aircraft_id AND t.kind = operations_checklist.kind);
