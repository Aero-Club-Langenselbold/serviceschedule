package de.ct.manager.schedule.maintenance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceWorkshopSlotRepository extends JpaRepository<MaintenanceWorkshopSlot, String> {

    @EntityGraph(attributePaths = "aircraft")
    List<MaintenanceWorkshopSlot> findAllByOrderByStartDateAscEndDateAsc();

    @EntityGraph(attributePaths = "aircraft")
    List<MaintenanceWorkshopSlot> findAllByAircraftIdOrderByStartDateAscEndDateAsc(String aircraftId);

    /** Whether another slot of the aircraft shares at least one day with the given period. */
    @Query("SELECT COUNT(s) > 0 FROM MaintenanceWorkshopSlot s WHERE s.aircraft = :aircraft AND s.id <> :excludedId "
            + "AND s.startDate <= :endDate AND s.endDate >= :startDate")
    boolean existsOverlap(MaintenanceAircraft aircraft, LocalDate startDate, LocalDate endDate, String excludedId);
}
