package de.ct.manager.schedule.operations;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OperationsChecklistItemRepository extends JpaRepository<OperationsChecklistItem, String> {

    @EntityGraph(attributePaths = { "checkedBy", "defect" })
    List<OperationsChecklistItem> findAllByChecklistOrderByPosition(OperationsChecklist checklist);

    @Query("SELECT i.checklist.id AS checklistId, COUNT(i) AS total, COUNT(i.result) AS checked, "
            + "SUM(CASE WHEN i.result = de.ct.manager.schedule.operations.ChecklistItemResult.NOT_OK THEN 1 ELSE 0 END) AS notOk "
            + "FROM OperationsChecklistItem i WHERE i.checklist IN :checklists GROUP BY i.checklist.id")
    List<Progress> progressOf(Collection<OperationsChecklist> checklists);

    interface Progress {
        String getChecklistId();
        long getTotal();
        long getChecked();
        long getNotOk();
    }
}
