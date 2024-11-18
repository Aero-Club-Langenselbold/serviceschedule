package de.ct.manager.schedule.plan;

import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(PlanController.class)
@Import({
    PlanService.class
})
class PlanControllerUnitTest extends AbstractControllerUnitTest{

    @MockBean
    private PlanRepository planRepository;

    @Test
    void testListPlans() throws Exception {
        when(planRepository.findAll()).thenReturn(generateListResponse());

        mockMvc.perform(
            get("/plan/list")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name", is("Anger Management 0")));
    }

    @Test
    void testPlan() throws Exception {
        when(planRepository.findById("4711")).thenReturn(Optional.of(generatePlan(0)));

        mockMvc.perform(
            get("/plan/fetch/4711")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")));
    }

    @Test
    void testCreatePlan() throws Exception {
        Plan plan = generatePlan(0);

        when(planRepository.save(plan)).thenReturn(plan);

        mockMvc.perform(
            post("/plan/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(plan))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")));
    }

    @Test
    void testDeletePlan() throws Exception {
        mockMvc.perform(
            delete("/plan/delete/4711")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk());
    }

    private List<Plan> generateListResponse() {
        List<Plan> plans = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            plans.add(generatePlan(i));
        }

        return plans;
    }

    private Plan generatePlan(int index) {
        Plan plan = new Plan();
        plan.setName("Anger Management " + index);
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        return plan;
    }
}
