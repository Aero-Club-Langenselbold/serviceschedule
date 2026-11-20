package de.ct.manager.schedule.maintenance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceAircraftRepository extends JpaRepository<MaintenanceAircraft, String> {

    List<MaintenanceAircraft> findAllByOrderByTypeAscRegistrationAsc();

    boolean existsByRegistrationIgnoreCase(String registration);

    boolean existsByRegistrationIgnoreCaseAndIdNot(String registration, String id);

    @Query("SELECT DISTINCT a.type FROM MaintenanceAircraft a ORDER BY a.type")
    List<String> findAllTypes();
}
