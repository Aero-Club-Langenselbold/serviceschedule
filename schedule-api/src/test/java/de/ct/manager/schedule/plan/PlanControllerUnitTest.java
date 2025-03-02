package de.ct.manager.schedule.plan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantAssignment;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import de.ct.manager.schedule.service.ServiceRepository;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(PlanController.class)
@Import({
    PlanService.class
})
class PlanControllerUnitTest extends AbstractControllerUnitTest{

    @MockitoBean
    private PlanRepository planRepository;

    @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private ParticipantRepository participantRepository;

    @Test
    void testListPlans() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);

        when(planRepository.findAll()).thenReturn(generateListResponse());

        mockMvc.perform(
            get("/plan/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name", is("Anger Management 0")));
    }

    @Test
    void testPlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);

        when(planRepository.findById("4711")).thenReturn(Optional.of(generatePlan(0)));

        mockMvc.perform(
            get("/plan/fetch/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")));
    }

    @Test
    void testCreatePlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);

        Plan plan = generatePlan(0);

        when(planRepository.save(plan)).thenReturn(plan);

        mockMvc.perform(
            post("/plan/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(plan))
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")));
    }

    @Test
    void testGeneratePlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);

        when(planRepository.findById("4711")).thenReturn(Optional.of(generatePlan(0)));
        when(serviceRepository.findById("4712")).thenReturn(Optional.of(generateService(0)));
        when(serviceRepository.findById("4713")).thenReturn(Optional.of(generateService(1)));
        when(participantRepository.findById("4714")).thenReturn(Optional.of(generateParticipant(0)));
        when(participantRepository.findById("4715")).thenReturn(Optional.of(generateParticipant(1)));

        // when a plan is saved return the plan that was saved
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(
            post("/plan/generate/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generatePlanDetails()))
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")))
            .andExpect(jsonPath("$.entries", hasSize(2)));
    }

    @Test
    void testDeletePlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        
        mockMvc.perform(
            delete("/plan/delete/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
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

    private PlanDetails generatePlanDetails() {
        PlanDetails planDetails = new PlanDetails();
        planDetails.setServiceDates(new ArrayList<ServiceDate>());
        planDetails.setAvailability(new ArrayList<ParticipantAssignment>());

        Service s1 = serviceRepository.findById("4712").get();
        Service s2 = serviceRepository.findById("4713").get();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, 01, 01, 0, 0, 0);

        ServiceDate serviceDate1 = new ServiceDate(calendar.getTime(), s1, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        ServiceDate serviceDate2 = new ServiceDate(calendar.getTime(), s2, 1);

        planDetails.getServiceDates().add(serviceDate1);
        planDetails.getServiceDates().add(serviceDate2);

        Participant p1 = participantRepository.findById("4714").get();
        p1.getServices().add(s1);
        List<Date> d1 = new ArrayList<>();
        d1.add(serviceDate1.getDate());
        Participant p2 = participantRepository.findById("4715").get();
        p2.getServices().add(s2);
        List<Date> d2 = new ArrayList<>();
        d2.add(serviceDate2.getDate());

        ParticipantAssignment pa1 = new ParticipantAssignment(p1, d1);
        ParticipantAssignment pa2 = new ParticipantAssignment(p2, d2);

        planDetails.getAvailability().add(pa1);
        planDetails.getAvailability().add(pa2);

        return planDetails;
    }

    private Plan generatePlan(int index) {
        Plan plan = new Plan();
        plan.setName("Anger Management " + index);
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        return plan;
    }

    private Service generateService(int index) {
        Service service = new Service();
        service.setServicename("Service No. " + index);

        return service;
    }

    private Participant generateParticipant(int index) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 01, 01, 0, 0, 0);

        Participant participant = new Participant();
        participant.setFirstname("Donald");
        participant.setLastname("Duck No. " + index);
        participant.setEmail("donald.duck@angermanagementissues.com");
        participant.setDateOfBirth(calendar.getTime());

        return participant;
    }
}
