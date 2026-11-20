package de.ct.manager.schedule.maintenance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceInventoryItemRepository extends JpaRepository<MaintenanceInventoryItem, String> {

    List<MaintenanceInventoryItem> findAllByOrderByNameAsc();
}
