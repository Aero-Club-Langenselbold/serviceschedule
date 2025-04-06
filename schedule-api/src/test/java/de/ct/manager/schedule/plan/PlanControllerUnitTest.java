package de.ct.manager.schedule.plan;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
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

import de.ct.manager.schedule.participant.DateParticipantAvailability;
import de.ct.manager.schedule.participant.DateParticipantAvailabilityRepository;
import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import de.ct.manager.schedule.service.ServiceDateRepository;
import de.ct.manager.schedule.service.ServiceRepository;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(PlanController.class)
@Import({
    PlanService.class
})
class PlanControllerUnitTest extends AbstractControllerUnitTest {

    @MockitoBean
    private PlanRepository planRepository;

    @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private ServiceDateRepository serviceDateRepository;

    @MockitoBean
    private DateParticipantAvailabilityRepository dateParticipantAvailabilityRepository;

    @Test
    void testListPlans() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        when(planRepository.findAll()).thenReturn(generateListResponse());

        mockMvc.perform(
            get("/plan/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name", is("Anger Management 0")));
    }

    @Test
    void testPlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        when(planRepository.findById("4711")).thenReturn(Optional.of(generatePlan(0)));

        mockMvc.perform(
            get("/plan/fetch/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")));
    }

    @Test
    void testCreatePlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        Plan plan = generatePlan(0);

        when(planRepository.save(plan)).thenReturn(plan);

        mockMvc.perform(
            post("/plan/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(plan))
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")));
    }

    @Test
    void testGeneratePlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        Plan plan = generatePlan(0);
        Service s1 = generateService(0);
        Service s2 = generateService(1);
        Participant p1 = generateParticipant(0);
        Participant p2 = generateParticipant(1);
        p1.setServices(List.of(s1));
        p2.setServices(List.of(s2));

        when(planRepository.findById("4711")).thenReturn(Optional.of(plan));
        when(serviceRepository.findById("4712")).thenReturn(Optional.of(s1));
        when(serviceRepository.findById("4713")).thenReturn(Optional.of(s2));
        when(serviceDateRepository.findByPlan(plan)).thenReturn(Optional.of(generateServiceDates(s1, s2)));
        when(participantRepository.findById("4714")).thenReturn(Optional.of(p1));
        when(participantRepository.findById("4715")).thenReturn(Optional.of(p2));
        when(dateParticipantAvailabilityRepository.findByPlan(plan)).thenReturn(Optional.of(generatePlanDetails(s1, s2, p1, p2)));

        // when a plan is saved return the plan that was saved
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(
            post("/plan/generate/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Anger Management 0")))
            .andExpect(jsonPath("$.entries", hasSize(2)));
    }

    @Test
    void testDeletePlan() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        mockMvc.perform(
            delete("/plan/delete/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
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

    private List<DateParticipantAvailability> generatePlanDetails(Service s1, Service s2, Participant p1, Participant p2) {
        List<DateParticipantAvailability> dpa = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, 01, 01, 0, 0, 0);

        ServiceDate serviceDate1 = new ServiceDate(calendar.getTime(), s1, 1, LocalTime.of(10, 0), LocalTime.of(12, 0));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        ServiceDate serviceDate2 = new ServiceDate(calendar.getTime(), s2, 1, LocalTime.of(10, 0), LocalTime.of(12, 0));

        dpa.add(new DateParticipantAvailability(serviceDate1.getDate(), false, p1, null));
        dpa.add(new DateParticipantAvailability(serviceDate2.getDate(), false, p2, null));

        return dpa;
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

    private List<ServiceDate> generateServiceDates(Service s1, Service s2) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, 01, 01, 0, 0, 0);

        ServiceDate serviceDate1 = new ServiceDate(calendar.getTime(), s1, 1, LocalTime.of(10, 0), LocalTime.of(12, 0));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        ServiceDate serviceDate2 = new ServiceDate(calendar.getTime(), s2, 1, LocalTime.of(10, 0), LocalTime.of(12, 0));

        return List.of(serviceDate1, serviceDate2);
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
