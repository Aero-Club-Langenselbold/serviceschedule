package de.ct.manager.schedule.winch;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WinchRopeDayRepository extends JpaRepository<WinchRopeDay, String> {

    @EntityGraph(attributePaths = { "rope", "updatedBy" })
    List<WinchRopeDay> findAllByDay(WinchDay day);

    Optional<WinchRopeDay> findByDayAndRope(WinchDay day, WinchRope rope);

    boolean existsByDay(WinchDay day);

    boolean existsByRope(WinchRope rope);

    @Query("SELECT DISTINCT r.rope.id FROM WinchRopeDay r")
    List<String> findUsedRopeIds();

    /** Ids of the days with at least one rope remark. */
    @Query("SELECT DISTINCT r.day.id FROM WinchRopeDay r WHERE r.day IN :days")
    List<String> findDayIdsWithRemarks(Collection<WinchDay> days);
}
