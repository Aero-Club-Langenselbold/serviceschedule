package de.ct.manager.schedule.maintenance;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceDefectEventRepository extends JpaRepository<MaintenanceDefectEvent, String> {

    @EntityGraph(attributePaths = { "actor", "subject" })
    List<MaintenanceDefectEvent> findAllByDefectOrderByCreatedAtAscIdAsc(MaintenanceDefect defect);

    @Query("SELECT e.defect.id AS defectId, COUNT(e) AS total FROM MaintenanceDefectEvent e "
            + "WHERE e.action = de.ct.manager.schedule.maintenance.DefectEventAction.COMMENTED AND e.defect IN :defects GROUP BY e.defect.id")
    List<CommentCounts> countCommentsByDefect(Collection<MaintenanceDefect> defects);

    interface CommentCounts {
        String getDefectId();
        long getTotal();
    }
}
