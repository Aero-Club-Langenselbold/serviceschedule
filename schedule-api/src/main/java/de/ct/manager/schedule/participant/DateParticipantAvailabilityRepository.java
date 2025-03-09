package de.ct.manager.schedule.participant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ct.manager.schedule.plan.Plan;

public interface DateParticipantAvailabilityRepository extends JpaRepository<DateParticipantAvailability, String> {
    Optional<List<DateParticipantAvailability>> findByPlan(Plan plan);

    void deleteByPlan(Plan plan);
}
