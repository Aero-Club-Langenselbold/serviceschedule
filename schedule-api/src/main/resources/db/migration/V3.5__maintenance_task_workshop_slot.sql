-- Every workshop visit gets its own board: a task belongs to one workshop period of its aircraft.
-- Tasks without a period (e.g. a defect fixed outside the workshop) stay on the board "without period" of the aircraft.
-- Deleting a period keeps its tasks, they move to that board.
ALTER TABLE maintenance_task ADD COLUMN workshop_slot_id character varying(36);
ALTER TABLE maintenance_task ADD CONSTRAINT fk_maintenance_task_workshop_slot
    FOREIGN KEY (workshop_slot_id) REFERENCES maintenance_workshop_slot (id) ON DELETE SET NULL;
CREATE INDEX idx_maintenance_task_workshop_slot ON maintenance_task (workshop_slot_id);

-- Until now all tasks of an aircraft formed one board, shown for every period. They move to the period
-- the UI showed the progress on: the one running today or else the next one; without such a period to the latest past one.
UPDATE maintenance_task SET workshop_slot_id = (
    SELECT s.id FROM maintenance_workshop_slot s
    WHERE s.aircraft_id = maintenance_task.aircraft_id
    ORDER BY CASE WHEN s.end_date >= CURRENT_DATE THEN 0 ELSE 1 END,
             CASE WHEN s.end_date >= CURRENT_DATE THEN s.start_date END ASC,
             s.end_date DESC
    FETCH FIRST 1 ROW ONLY
);
