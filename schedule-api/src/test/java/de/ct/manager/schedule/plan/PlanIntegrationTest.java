package de.ct.manager.schedule.plan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import de.ct.manager.schedule.participant.DateParticipantAvailability;
import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
class PlanIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-plans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testListPlans() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<List<Plan>> response = restTemplate.exchange(
            createURLWithPort("plan/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Plan>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).isNotEmpty();
        Assertions.assertThat(response.getBody()).hasSize(10);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-plans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testFetchPlan() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<Plan> response = restTemplate.exchange(
            createURLWithPort("plan/") + "fetch/6000",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();

        Plan plan = response.getBody();
        Assertions.assertThat(plan.getName()).isEqualTo("World Domination tour");
        Assertions.assertThat(plan.getEntries()).isNotNull().isNotEmpty().hasSize(3);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-plans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testDeletePlan() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        restTemplate.exchange(
            createURLWithPort("plan/") + "delete/6000",
            HttpMethod.DELETE,
            entity,
            new ParameterizedTypeReference<Void>() {});

        // verify deletion
        ResponseEntity<List<Plan>> response = restTemplate.exchange(
            createURLWithPort("plan/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Plan>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(9);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-plans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCreatePlan() throws Exception {

        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        // get required service and participant
        ResponseEntity<Service> serviceResponse = restTemplate.exchange(
            createURLWithPort("service/") + "fetch/5111",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Service>() {});

        Assertions.assertThat(serviceResponse).isNotNull();
        Assertions.assertThat(serviceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(serviceResponse.getBody()).isNotNull();

        ResponseEntity<Participant> participantResponse = restTemplate.exchange(
            createURLWithPort("participant/") + "fetch/4711",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Participant>() {});

        Assertions.assertThat(participantResponse).isNotNull();
        Assertions.assertThat(participantResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(participantResponse.getBody()).isNotNull();

        // build new plan
        Plan newby = new Plan();
        newby.setName("Anger Management");
        newby.setDescription("What the world could look like if we all just got along.");
        newby.setDateFrom(new Date());
        newby.setDateUntil(new Date());
        newby.setEntries(new HashSet<>());

        PlanEntry entry = new PlanEntry();
        entry.setPlan(newby);
        entry.setStartTime(LocalDateTime.now());
        entry.setEndTime(LocalDateTime.now().plusHours(1));
        entry.setService(serviceResponse.getBody());
        entry.setParticipant(participantResponse.getBody());
        newby.getEntries().add(entry);

        entity = new HttpEntity<>(objectMapper.writeValueAsString(newby), createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<Plan> plan = restTemplate.exchange(
            createURLWithPort("plan/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(plan.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(plan.getBody()).isNotNull();

        // verify creation
        ResponseEntity<List<Plan>> response = restTemplate.exchange(
            createURLWithPort("plan/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Plan>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(11);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-plans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdatePlan() throws Exception {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        // fetch a plan
        ResponseEntity<Plan> response = restTemplate.exchange(
            createURLWithPort("plan/") + "fetch/6000",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getName()).isEqualTo("World Domination tour");

        // update plan
        Plan plan = response.getBody();
        plan.setName("Universe Domination tour");

        entity = new HttpEntity<>(objectMapper.writeValueAsString(plan), createHeadersWithToken(UserRole.ROLE_ADMIN));

        response = restTemplate.exchange(
            createURLWithPort("plan/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();

        // verify plan
        response = restTemplate.exchange(
            createURLWithPort("plan/") + "fetch/6000",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getName()).isEqualTo("Universe Domination tour");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-participants-full.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGeneratePlan() throws Exception {
        // create new plan
        Plan newby = new Plan();
        newby.setName("Anger Management");
        newby.setDescription("What the world could look like if we all just got along.");
        newby.setDateFrom(new Date());
        newby.setDateUntil(new Date());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(newby), createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<Plan> plan = restTemplate.exchange(
            createURLWithPort("plan/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(plan.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(plan.getBody()).isNotNull();

        // get particpants and services
        ResponseEntity<List<Service>> serviceResponse = restTemplate.exchange(
            createURLWithPort("service/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Service>>() {});

        Assertions.assertThat(serviceResponse).isNotNull();
        Assertions.assertThat(serviceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(serviceResponse.getBody()).isNotEmpty();

        ResponseEntity<List<Participant>> participantResponse = restTemplate.exchange(
            createURLWithPort("participant/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Participant>>() {});

        Assertions.assertThat(participantResponse).isNotNull();
        Assertions.assertThat(participantResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(participantResponse.getBody()).isNotEmpty();

        List<Service> services = serviceResponse.getBody();
        List<Participant> participants = participantResponse.getBody();

        // generate plan entries
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, 01, 01, 0, 0, 0);
        // add service dates
        List<ServiceDate> sds = new ArrayList<>();
        for (Service s : services) {
            ServiceDate sd = new ServiceDate(calendar.getTime(), s, 1, LocalTime.of(10, 0), LocalTime.of(12, 0));
            sd.setPlan(plan.getBody());
            sds.add(sd);
        }
        entity = new HttpEntity<>(objectMapper.writeValueAsString(sds), createHeadersWithToken(UserRole.ROLE_ADMIN));

        restTemplate.exchange(
            createURLWithPort("plan/") + "/serviceDates/save/" + plan.getBody().getId(),
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<List<ServiceDate>>() {});
        // add availability dates
        for (Participant p : participants) {
            DateParticipantAvailability availability = new DateParticipantAvailability(calendar.getTime(), false, p, plan.getBody());
            entity = new HttpEntity<>(objectMapper.writeValueAsString(List.of(availability)), createHeadersWithToken(UserRole.ROLE_ADMIN));

            restTemplate.exchange(
                createURLWithPort("plan/") + "/participantAvailability/save/" + plan.getBody().getId(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<DateParticipantAvailability>() {});
        }

        entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        plan = restTemplate.exchange(
            createURLWithPort("plan/") + "generate/" + plan.getBody().getId(),
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(plan.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(plan.getBody()).isNotNull();
        Assertions.assertThat(plan.getBody().getEntries()).hasSize(5);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-duplicate-assignment.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-duplicate-assignment.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGeneratePlanWithoutDuplicateAssignments() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<Plan> plan = restTemplate.exchange(
            createURLWithPort("plan/") + "generate/bb883bd7-53cb-4c1f-8596-b0ef222211f3", // plan id as in test-duplicate-assignment.sql
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Plan>() {});

        Assertions.assertThat(plan.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(plan.getBody()).isNotNull();

        // bug report: on April 5th, 2025 the same person was assigned to two different services at the same time
        Plan p = plan.getBody();
        long uniqueEmailCount = p.getEntries().stream()
            .filter(entry -> entry.getDate().equals(LocalDate.of(2025, 4, 5)))
            .map(entry -> entry.getParticipant().getEmail())
            .distinct()
            .count();

        long entryCount = p.getEntries().stream()
            .filter(entry -> entry.getDate().equals(LocalDate.of(2025, 4, 5)))
            .count();

        Assertions.assertThat(uniqueEmailCount).isEqualTo(entryCount);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-plans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPlanStatistics() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        // fetch a plan
        ResponseEntity<List<PlanStatistics>> response = restTemplate.exchange(
            createURLWithPort("plan/") + "statistics/6000",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<PlanStatistics>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(3);
    }
}
