package de.ct.manager.schedule.plan;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class PlanServiceUnitTest {

    @Mock
    PlanRepository planRepository;

    @InjectMocks
    PlanService planService;

    @Test
    void testLoadPlanList() {
        when(planRepository.findAll()).thenReturn(generateListResponse());

        List<Plan> result = planService.listPlans();

        Assertions.assertThat(result).isNotNull().hasSize(10);
    }

    @Test
    void testGetPlan() {
        Plan plan = new Plan();
        plan.setName("Anger Management");
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        when(planRepository.findById("1")).thenReturn(Optional.of(plan));
        when(planRepository.findById("0")).thenReturn(Optional.empty());

        Assertions.assertThat(planService.getPlan("1")).isNotNull();
        Assertions.assertThat(planService.getPlan("0")).isNull();
    }

    @Test
    void testCreateOrUpdatePlan() {
        Plan plan = new Plan();
        plan.setName("Anger Management");
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        when(planRepository.save(plan)).thenReturn(plan);

        Assertions.assertThat(planService.createOrUpdatePlan(plan)).isNotNull();
    }

    @Test
    void testDeleteParticipant() {
        Plan plan = new Plan();
        plan.setName("Anger Management");
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        when(planRepository.findById("1")).thenReturn(Optional.of(plan));
        when(planRepository.findById("0")).thenReturn(Optional.empty());

        planService.deletePlan("1");
        planService.deletePlan("0");

        Assertions.assertThatNoException();
    }

    private List<Plan> generateListResponse() {
        List<Plan> plans = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Plan plan = new Plan();
            plan.setName("Anger Management " + i);
            plan.setDescription("What the world could look like if we all just got along.");
            plan.setDateFrom(new Date());
            plan.setDateUntil(new Date());
            plans.add(plan);
        }

        return plans;
    }
}
