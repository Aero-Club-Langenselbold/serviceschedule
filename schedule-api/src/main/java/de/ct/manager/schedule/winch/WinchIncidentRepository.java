package de.ct.manager.schedule.winch;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WinchIncidentRepository extends JpaRepository<WinchIncident, String> {

    @EntityGraph(attributePaths = { "rope", "type", "driver" })
    List<WinchIncident> findAllByDayOrderByCreatedAt(WinchDay day);

    boolean existsByDay(WinchDay day);

    boolean existsByRope(WinchRope rope);

    boolean existsByType(WinchIncidentType type);

    @Query("SELECT DISTINCT i.rope.id FROM WinchIncident i")
    List<String> findUsedRopeIds();

    @Query("SELECT DISTINCT i.type.id FROM WinchIncident i")
    List<String> findUsedTypeIds();

    /** Incidents per day and kind, for the list of days. */
    @Query("SELECT i.day.id AS dayId, t.id AS typeId, t.name AS typeName, t.position AS sortOrder, COUNT(i) AS total "
            + "FROM WinchIncident i JOIN i.type t WHERE i.day IN :days GROUP BY i.day.id, t.id, t.name, t.position")
    List<TypeCount> countByDayAndType(Collection<WinchDay> days);

    interface TypeCount {
        String getDayId();
        String getTypeId();
        String getTypeName();
        int getSortOrder();
        long getTotal();
    }
}
