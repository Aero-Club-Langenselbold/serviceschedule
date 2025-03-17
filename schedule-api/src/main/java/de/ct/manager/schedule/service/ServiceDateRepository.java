package de.ct.manager.schedule.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ct.manager.schedule.plan.Plan;

public interface ServiceDateRepository extends JpaRepository<ServiceDate, String> {

    Optional<List<ServiceDate>> findByPlan(Plan plan);

    Optional<List<ServiceDate>> findByServiceId(String serviceId);
}
