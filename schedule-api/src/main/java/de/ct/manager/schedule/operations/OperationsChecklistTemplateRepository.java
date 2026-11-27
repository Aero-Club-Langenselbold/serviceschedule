package de.ct.manager.schedule.operations;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import de.ct.manager.schedule.maintenance.MaintenanceAircraft;

public interface OperationsChecklistTemplateRepository extends JpaRepository<OperationsChecklistTemplate, String> {

    @EntityGraph(attributePaths = { "items" })
    Optional<OperationsChecklistTemplate> findByAircraftAndKind(MaintenanceAircraft aircraft, ChecklistKind kind);

    @EntityGraph(attributePaths = { "aircraft", "items" })
    List<OperationsChecklistTemplate> findAllBy();
}
