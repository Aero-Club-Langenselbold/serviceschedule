package de.ct.manager.schedule.maintenance;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceDefectRepository extends JpaRepository<MaintenanceDefect, String> {

    @EntityGraph(attributePaths = { "aircraft", "reportedBy", "doneBy", "signedBy", "assignees" })
    List<MaintenanceDefect> findAllByAircraftOrderByReportedAtDesc(MaintenanceAircraft aircraft);

    @EntityGraph(attributePaths = { "aircraft", "reportedBy", "doneBy", "signedBy", "assignees" })
    List<MaintenanceDefect> findAllByOrderByReportedAtDesc();

    /** Open defects are those still to be worked on: OPEN and IN_PROGRESS. */
    @Query("SELECT d.aircraft.id AS aircraftId, COUNT(d) AS total FROM MaintenanceDefect d "
            + "WHERE d.status IN (de.ct.manager.schedule.maintenance.DefectStatus.OPEN, de.ct.manager.schedule.maintenance.DefectStatus.IN_PROGRESS) "
            + "GROUP BY d.aircraft.id")
    List<OpenDefectCounts> countOpenByAircraft();

    interface OpenDefectCounts {
        String getAircraftId();
        long getTotal();
    }
}
