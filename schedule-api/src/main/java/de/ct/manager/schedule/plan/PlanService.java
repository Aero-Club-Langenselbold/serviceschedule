package de.ct.manager.schedule.plan;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<Plan> listPlans() {
        return planRepository.findAll();
    }

    public Plan getPlan(String id) {
        return planRepository.findById(id).orElse(null);
    }

    public Plan createOrUpdatePlan(Plan plan) {
        return planRepository.save(plan);
    }

    public void deletePlan(String id) {
        planRepository.deleteById(id);
    }
}
