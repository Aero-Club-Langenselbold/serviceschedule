package de.ct.manager.schedule.participant;

import java.util.Date;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import de.ct.manager.schedule.util.AbstractIntegrationTest;
import de.ct.manager.schedule.util.RestPageImpl;

class ParticipantIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-participants.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageParticipantsWithoutParams() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        ResponseEntity<RestPageImpl<Participant>> response = restTemplate.exchange(
            createURLWithPort("participant/") + "page",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<RestPageImpl<Participant>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getNumberOfElements()).isEqualTo(20);
        Assertions.assertThat(response.getBody().getContent()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-participants.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageParticipantsWithParams() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        ResponseEntity<RestPageImpl<Participant>> response = restTemplate.exchange(
            createURLWithPort("participant/") + "page?page=0&size=5&sort=firstname,desc",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<RestPageImpl<Participant>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getNumberOfElements()).isEqualTo(5);
        Assertions.assertThat(response.getBody().getContent()).isNotEmpty();
        Assertions.assertThat(response.getBody().getContent().getFirst().getFirstname()).isEqualTo("Scrooge");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-participants.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testListParticipants() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        ResponseEntity<List<Participant>> response = restTemplate.exchange(
            createURLWithPort("participant/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Participant>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(21);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-participants.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testFetchParticipant() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        ResponseEntity<Participant> response = restTemplate.exchange(
            createURLWithPort("participant/") + "fetch/4711",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Participant>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getEmail()).isEqualTo("donald@angermanagementissues.com");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-participants.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testDeleteParticipant() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        restTemplate.exchange(
            createURLWithPort("participant/") + "delete/4711",
            HttpMethod.DELETE,
            entity,
            new ParameterizedTypeReference<Void>() {});

        // verify deletion
        ResponseEntity<List<Participant>> response = restTemplate.exchange(
            createURLWithPort("participant/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Participant>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(20);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCreateParticipant() throws Exception {
        Participant newby = new Participant();
        newby.setFirstname("Donald");
        newby.setLastname("Duck");
        newby.setEmail("therealdonald@theworld.com");
        newby.setDateOfBirth(new Date());
        newby.setStatus(Status.ACTIVE);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(newby), createHeaders());

        ResponseEntity<Participant> participant = restTemplate.exchange(
            createURLWithPort("participant/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Participant>() {});

        Assertions.assertThat(participant.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(participant.getBody()).isNotNull();

        // verify creation
        ResponseEntity<List<Participant>> response = restTemplate.exchange(
            createURLWithPort("participant/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Participant>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(1);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-participants.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdateParticipant() throws Exception {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        // fetch donald
        ResponseEntity<Participant> response = restTemplate.exchange(
            createURLWithPort("participant/") + "fetch/4711",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Participant>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getEmail()).isEqualTo("donald@angermanagementissues.com");

        // update donald
        Participant participant = response.getBody();
        participant.setEmail("donald@notangryanymore.com");

        entity = new HttpEntity<>(objectMapper.writeValueAsString(participant), createHeaders());

        response = restTemplate.exchange(
            createURLWithPort("participant/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Participant>() {});

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();

        // verify donald
        response = restTemplate.exchange(
            createURLWithPort("participant/") + "fetch/4711",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Participant>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getEmail()).isEqualTo("donald@notangryanymore.com");
    }
}
