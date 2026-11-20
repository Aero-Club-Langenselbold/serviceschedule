package de.ct.manager.schedule.maintenance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, String> {

    @EntityGraph(attributePaths = { "doneBy", "signedBy", "defect" })
    List<MaintenanceTask> findAllByAircraftOrderByLevelAscTitleAsc(MaintenanceAircraft aircraft);

    Optional<MaintenanceTask> findByDefect(MaintenanceDefect defect);

    List<MaintenanceTask> findAllByDefectIn(List<MaintenanceDefect> defects);

    @Query("SELECT t.template.id FROM MaintenanceTask t WHERE t.aircraft = :aircraft AND t.template IS NOT NULL")
    List<String> findTemplateIdsByAircraft(MaintenanceAircraft aircraft);

    /** Per aircraft: number of tasks, finished tasks and signed off tasks. */
    @Query("SELECT t.aircraft.id AS aircraftId, COUNT(t) AS total, "
            + "SUM(CASE WHEN t.status = de.ct.manager.schedule.maintenance.TaskStatus.DONE THEN 1 ELSE 0 END) AS done, "
            + "SUM(CASE WHEN t.signedAt IS NOT NULL THEN 1 ELSE 0 END) AS signed "
            + "FROM MaintenanceTask t GROUP BY t.aircraft.id")
    List<TaskCounts> countByAircraft();

    interface TaskCounts {
        String getAircraftId();
        long getTotal();
        long getDone();
        long getSigned();
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
