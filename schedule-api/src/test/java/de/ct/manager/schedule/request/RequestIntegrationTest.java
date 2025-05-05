package de.ct.manager.schedule.request;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;
import de.ct.manager.schedule.util.RestPageImpl;

@SuppressWarnings("null") // null warnings in assertions are false-positives
class RequestIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageRequestsWithoutParams() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<RestPageImpl<Request>> response = restTemplate.exchange(
            createURLWithPort("request/") + "page",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<RestPageImpl<Request>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getNumberOfElements()).isEqualTo(5);
        Assertions.assertThat(response.getBody().getContent()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageRequestsWithoutParamsAsUser() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_USER));

        ResponseEntity<RestPageImpl<Request>> response = restTemplate.exchange(
            createURLWithPort("request/") + "page",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<RestPageImpl<Request>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getNumberOfElements()).isEqualTo(0);
        Assertions.assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageRequestsWithParams() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<RestPageImpl<Request>> response = restTemplate.exchange(
            createURLWithPort("request/") + "page?page=0&size=3&sort=createdAt,desc",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<RestPageImpl<Request>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getNumberOfElements()).isEqualTo(3);
        Assertions.assertThat(response.getBody().getContent()).isNotEmpty();
        Assertions.assertThat(response.getBody().getContent().getFirst().getHeadline()).isEqualTo("Bug in Login");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testListRequests() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<List<Request>> response = restTemplate.exchange(
            createURLWithPort("request/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Request>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(5);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testListRequestsAsUser() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_USER));

        ResponseEntity<List<Request>> response = restTemplate.exchange(
            createURLWithPort("request/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Request>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(0);
        Assertions.assertThat(response.getBody()).isEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testFetchRequest() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<Request> response = restTemplate.exchange(
            createURLWithPort("request/") + "fetch/3c4d5e6f-7890-12cd-efgh-3456789012cd",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Request>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getHeadline()).isEqualTo("UI Improvement: Button Alignment");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testDeleteRequest() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        restTemplate.exchange(
            createURLWithPort("request/") + "delete/3c4d5e6f-7890-12cd-efgh-3456789012cd",
            HttpMethod.DELETE,
            entity,
            new ParameterizedTypeReference<Void>() {});

        // verify deletion
        ResponseEntity<List<Request>> response = restTemplate.exchange(
            createURLWithPort("request/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Request>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(4);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCreateRequest() throws Exception {
        Request newby = new Request();
        newby.setHeadline("Anger Management");
        newby.setDescription("What the world could look like if we all just got along.");

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(newby), createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<Request> request = restTemplate.exchange(
            createURLWithPort("request/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Request>() {});

        Assertions.assertThat(request.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(request.getBody()).isNotNull();

        // verify creation
        ResponseEntity<List<Request>> response = restTemplate.exchange(
            createURLWithPort("request/") + "list",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Request>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).hasSize(1);
        Assertions.assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdateRequest() throws Exception {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        // fetch
        ResponseEntity<Request> response = restTemplate.exchange(
            createURLWithPort("request/") + "fetch/1a2b3c4d-5678-90ab-cdef-1234567890ab",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Request>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getHeadline()).isEqualTo("Bug in Login");

        // update
        Request request = response.getBody();
        request.setHeadline("Major bug in Login");

        entity = new HttpEntity<>(objectMapper.writeValueAsString(request), createHeadersWithToken(UserRole.ROLE_ADMIN));

        response = restTemplate.exchange(
            createURLWithPort("request/") + "save",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<Request>() {});

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();

        // verify
        response = restTemplate.exchange(
            createURLWithPort("request/") + "fetch/1a2b3c4d-5678-90ab-cdef-1234567890ab",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Request>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getHeadline()).isEqualTo("Major bug in Login");
    }
}
