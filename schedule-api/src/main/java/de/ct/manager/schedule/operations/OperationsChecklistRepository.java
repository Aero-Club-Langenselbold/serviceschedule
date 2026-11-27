package de.ct.manager.schedule.operations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.ct.manager.schedule.maintenance.MaintenanceAircraft;

public interface OperationsChecklistRepository extends JpaRepository<OperationsChecklist, String> {

    Optional<OperationsChecklist> findByAircraftAndKindAndDate(MaintenanceAircraft aircraft, ChecklistKind kind, LocalDate date);

    @EntityGraph(attributePaths = { "aircraft", "createdBy", "submittedBy", "approvedBy" })
    List<OperationsChecklist> findAllByDate(LocalDate date);

    String ARCHIVE_FILTER = " FROM OperationsChecklist c JOIN c.aircraft a LEFT JOIN c.createdBy u "
            + "WHERE (:aircraftId IS NULL OR a.id = :aircraftId) AND (:kind IS NULL OR c.kind = :kind) AND (:status IS NULL OR c.status = :status) "
            + "AND (:dateFrom IS NULL OR c.date >= :dateFrom) AND (:dateTo IS NULL OR c.date <= :dateTo) "
            + "AND (:search IS NULL OR LOWER(a.registration) LIKE :search OR LOWER(a.type) LIKE :search OR LOWER(u.username) LIKE :search "
            + "OR EXISTS (SELECT i.id FROM OperationsChecklistItem i WHERE i.checklist = c AND (LOWER(i.comment) LIKE :search OR LOWER(i.text) LIKE :search)))";

    /**
     * The archive. Every filter is optional; search is a lower case LIKE pattern matched against registration and type of
     * the aircraft, the username of the initial recorder and the text and comments of the items.
     */
    @EntityGraph(attributePaths = { "aircraft", "createdBy", "submittedBy", "approvedBy" })
    @Query(value = "SELECT c" + ARCHIVE_FILTER, countQuery = "SELECT COUNT(c)" + ARCHIVE_FILTER)
    Page<OperationsChecklist> search(String aircraftId, ChecklistKind kind, ChecklistStatus status, LocalDate dateFrom, LocalDate dateTo,
            String search, Pageable pageable);
}
