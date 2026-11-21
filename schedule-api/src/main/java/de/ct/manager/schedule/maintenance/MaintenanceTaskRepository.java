package de.ct.manager.schedule.maintenance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, String> {

    /** The board of a workshop visit. */
    @EntityGraph(attributePaths = { "doneBy", "signedBy", "defect", "template" })
    List<MaintenanceTask> findAllByWorkshopSlotOrderByLevelAscTitleAsc(MaintenanceWorkshopSlot workshopSlot);

    /** The board of an aircraft without workshop period. */
    @EntityGraph(attributePaths = { "doneBy", "signedBy", "defect", "template" })
    List<MaintenanceTask> findAllByAircraftAndWorkshopSlotIsNullOrderByLevelAscTitleAsc(MaintenanceAircraft aircraft);

    Optional<MaintenanceTask> findByDefect(MaintenanceDefect defect);

    List<MaintenanceTask> findAllByDefectIn(List<MaintenanceDefect> defects);

    boolean existsByWorkshopSlot(MaintenanceWorkshopSlot workshopSlot);

    @Query("SELECT t.template.id FROM MaintenanceTask t WHERE t.workshopSlot = :workshopSlot AND t.template IS NOT NULL")
    List<String> findTemplateIdsByWorkshopSlot(MaintenanceWorkshopSlot workshopSlot);

    @Query("SELECT t.template.id FROM MaintenanceTask t WHERE t.aircraft = :aircraft AND t.workshopSlot IS NULL AND t.template IS NOT NULL")
    List<String> findUnscheduledTemplateIdsByAircraft(MaintenanceAircraft aircraft);

    /** Per aircraft: number of tasks without workshop period. */
    @Query("SELECT t.aircraft.id AS aircraftId, COUNT(t) AS total FROM MaintenanceTask t WHERE t.workshopSlot IS NULL GROUP BY t.aircraft.id")
    List<UnscheduledCounts> countUnscheduledByAircraft();

    interface UnscheduledCounts {
        String getAircraftId();
        long getTotal();
    }

    /** Per workshop visit: number of tasks, finished tasks and signed off tasks. */
    @Query("SELECT t.workshopSlot.id AS slotId, COUNT(t) AS total, "
            + "SUM(CASE WHEN t.status = de.ct.manager.schedule.maintenance.TaskStatus.DONE THEN 1 ELSE 0 END) AS done, "
            + "SUM(CASE WHEN t.signedAt IS NOT NULL THEN 1 ELSE 0 END) AS signed "
            + "FROM MaintenanceTask t WHERE t.workshopSlot IS NOT NULL GROUP BY t.workshopSlot.id")
    List<TaskCounts> countBySlot();

    interface TaskCounts {
        String getSlotId();
        long getTotal();
        long getDone();
        long getSigned();
    }

    /** Per workshop visit: checklist steps of its tasks and how many of them are ticked. */
    @Query("SELECT c.task.workshopSlot.id AS slotId, COUNT(c) AS total, SUM(CASE WHEN c.done = true THEN 1 ELSE 0 END) AS done "
            + "FROM MaintenanceTaskChecklistItem c WHERE c.task.workshopSlot IS NOT NULL GROUP BY c.task.workshopSlot.id")
    List<StepCounts> countChecklistStepsBySlot();

    /** Per workshop visit: tasks without a checklist and how many of them are done; each counts as a single step. */
    @Query("SELECT t.workshopSlot.id AS slotId, COUNT(t) AS total, "
            + "SUM(CASE WHEN t.status = de.ct.manager.schedule.maintenance.TaskStatus.DONE THEN 1 ELSE 0 END) AS done "
            + "FROM MaintenanceTask t WHERE t.workshopSlot IS NOT NULL AND t.checklist IS EMPTY GROUP BY t.workshopSlot.id")
    List<StepCounts> countTasksWithoutChecklistBySlot();

    interface StepCounts {
        String getSlotId();
        long getTotal();
        long getDone();
    }

    /** Checklist progress per task of an aircraft. */
    @Query("SELECT c.task.id AS taskId, COUNT(c) AS total, SUM(CASE WHEN c.done = true THEN 1 ELSE 0 END) AS done "
            + "FROM MaintenanceTaskChecklistItem c WHERE c.task.aircraft = :aircraft GROUP BY c.task.id")
    List<ChecklistCounts> countChecklistByAircraft(MaintenanceAircraft aircraft);

    interface ChecklistCounts {
        String getTaskId();
        long getTotal();
        long getDone();
    }

    @Query("SELECT n.task.id AS taskId, COUNT(n) AS total FROM MaintenanceTaskNote n WHERE n.task.aircraft = :aircraft GROUP BY n.task.id")
    List<NoteCounts> countNotesByAircraft(MaintenanceAircraft aircraft);

    interface NoteCounts {
        String getTaskId();
        long getTotal();
    }
}
