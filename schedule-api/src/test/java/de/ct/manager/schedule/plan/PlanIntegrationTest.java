package de.ct.manager.schedule.plan;

import java.time.LocalDateTime;
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

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

class PlanIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-plans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-test-plans.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testListPlans() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

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
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

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
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

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

        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

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

        entity = new HttpEntity<>(objectMapper.writeValueAsString(newby), createHeaders());

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
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

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

        entity = new HttpEntity<>(objectMapper.writeValueAsString(plan), createHeaders());

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
}
