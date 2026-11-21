package de.ct.manager.schedule.plan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, String> {

    /** Newest plan first: by start, then end (both descending), then name. */
    List<Plan> findAllByOrderByDateFromDescDateUntilDescNameAsc();
}
