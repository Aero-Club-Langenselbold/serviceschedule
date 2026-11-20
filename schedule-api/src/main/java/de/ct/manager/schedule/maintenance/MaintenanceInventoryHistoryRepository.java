package de.ct.manager.schedule.maintenance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceInventoryHistoryRepository extends JpaRepository<MaintenanceInventoryHistory, String> {

    @EntityGraph(attributePaths = "actor")
    List<MaintenanceInventoryHistory> findAllByItemOrderByCreatedAtDesc(MaintenanceInventoryItem item);

    @EntityGraph(attributePaths = "actor")
    Optional<MaintenanceInventoryHistory> findFirstByItemOrderByCreatedAtDesc(MaintenanceInventoryItem item);
}
