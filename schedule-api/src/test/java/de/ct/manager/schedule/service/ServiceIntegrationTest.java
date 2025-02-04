package de.ct.manager.schedule.service;

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

@SuppressWarnings("null") // null warnings in assertions are false-positives
class ServiceIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-services.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM service", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testListServices() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        ResponseEntity<List<Service>> response = restTemplate.exchange(
            createURLWithPort("service/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Service>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).isNotEmpty();
        Assertions.assertThat(response.getBody()).hasSize(5);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-services.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM service", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testFetchService() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        ResponseEntity<Service> response = restTemplate.exchange(
            createURLWithPort("service/") + "fetch/5111",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Service>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getServicename()).isEqualTo("Funky Service");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-services.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM service", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testDeleteService() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        restTemplate.exchange(
            createURLWithPort("service/") + "delete/5111",
            HttpMethod.DELETE,
            entity,
            new ParameterizedTypeReference<Void>() {});

        // verify deletion
        ResponseEntity<List<Service>> response = restTemplate.exchange(
            createURLWithPort("service/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Service>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(4);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(statements = "DELETE FROM service", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCreateService() throws Exception {
        Service newby = new Service();
        newby.setServicename("Nullpointer service");

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(newby), createHeaders());

        ResponseEntity<Service> service = restTemplate.exchange(
            createURLWithPort("service/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Service>() {});

        Assertions.assertThat(service.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(service.getBody()).isNotNull();

        // verify creation
        ResponseEntity<List<Service>> response = restTemplate.exchange(
            createURLWithPort("service/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Service>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(1);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-services.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM service", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdateService() throws Exception {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeaders());

        // fetch funky service
        ResponseEntity<Service> response = restTemplate.exchange(
            createURLWithPort("service/") + "fetch/5111",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Service>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getServicename()).isEqualTo("Funky Service");

        // update funky service
        Service service = response.getBody();
        service.setServicename("Even funkier Service");

        entity = new HttpEntity<>(objectMapper.writeValueAsString(service), createHeaders());

        response = restTemplate.exchange(
            createURLWithPort("service/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Service>() {});

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();

        // verify funky service
        response = restTemplate.exchange(
            createURLWithPort("service/") + "fetch/5111",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Service>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getServicename()).isEqualTo("Even funkier Service");
    }
}
