package de.ct.manager.schedule.plan;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanEntryRepository extends JpaRepository<PlanEntry, String> {

    /** The services of a participant in published plans that end at or after {@code from}. */
    @Query("""
        SELECT e FROM PlanEntry e JOIN FETCH e.service JOIN FETCH e.plan p
        WHERE e.participant.email = :email AND p.published = true AND e.endTime >= :from
        ORDER BY e.startTime, e.service.servicename""")
    List<PlanEntry> findPublishedByParticipantEmail(@Param("email") String email, @Param("from") LocalDateTime from);

    /** Everybody assigned in the given plans, to tell a participant who else is on duty. */
    @Query("SELECT e FROM PlanEntry e JOIN FETCH e.participant WHERE e.plan.id IN :planIds")
    List<PlanEntry> findAssignedByPlanIds(@Param("planIds") Collection<String> planIds);
}
