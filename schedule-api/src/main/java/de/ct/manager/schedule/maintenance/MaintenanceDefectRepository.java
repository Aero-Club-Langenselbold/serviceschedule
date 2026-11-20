package de.ct.manager.schedule.maintenance;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceDefectRepository extends JpaRepository<MaintenanceDefect, String> {

    @EntityGraph(attributePaths = { "reportedBy", "doneBy" })
    List<MaintenanceDefect> findAllByAircraftOrderByReportedAtDesc(MaintenanceAircraft aircraft);

    @Query("SELECT d.aircraft.id AS aircraftId, COUNT(d) AS total FROM MaintenanceDefect d "
            + "WHERE d.status = de.ct.manager.schedule.maintenance.DefectStatus.OPEN GROUP BY d.aircraft.id")
    List<OpenDefectCounts> countOpenByAircraft();

    interface OpenDefectCounts {
        String getAircraftId();
        long getTotal();
    }
}
