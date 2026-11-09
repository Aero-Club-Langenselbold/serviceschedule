package de.ct.manager.schedule.request;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
class RequestCommentIntegrationTest extends AbstractIntegrationTest {

    private static final String OPEN_BUG = "1a2b3c4d-5678-90ab-cdef-1234567890ab";
    private static final String IN_PROGRESS = "2b3c4d5e-6789-01bc-defg-2345678901bc";
    private static final String CLOSED = "3c4d5e6f-7890-12cd-efgh-3456789012cd";

    private ResponseEntity<String> addComment(String requestId, UserRole role, String body) {
        return restTemplate.exchange(createURLWithPort("request/" + requestId + "/comments"), HttpMethod.POST, new HttpEntity<>(body, createHeadersWithToken(role)), String.class);
    }

    private ResponseEntity<String> getComments(String requestId, UserRole role) {
        return restTemplate.exchange(createURLWithPort("request/" + requestId + "/comments"), HttpMethod.GET, new HttpEntity<>(null, createHeadersWithToken(role)), String.class);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAddAndListCommentsInOrder() throws Exception {
        Assertions.assertThat(addComment(OPEN_BUG, UserRole.ROLE_ADMIN, "{\"content\":\"  first  \"}").getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(addComment(OPEN_BUG, UserRole.ROLE_ADMIN, "{\"content\":\"second\"}").getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> response = getComments(OPEN_BUG, UserRole.ROLE_ADMIN);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode comments = objectMapper.readTree(response.getBody());
        Assertions.assertThat(comments).hasSize(2);
        Assertions.assertThat(comments.get(0).get("content").asText()).isEqualTo("first");
        Assertions.assertThat(comments.get(0).get("author").asText()).isEqualTo("admin@test.de");
        // with time of day, the UI shows when a comment was written
        Assertions.assertThat(comments.get(0).get("createdAt").asText()).contains("T");
        Assertions.assertThat(comments.get(1).get("content").asText()).isEqualTo("second");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testRequestsInProgressCountAsOpen() {
        Assertions.assertThat(addComment(IN_PROGRESS, UserRole.ROLE_ADMIN, "{\"content\":\"on it\"}").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testClosedRequestsCannotBeCommentedButStayReadable() {
        Assertions.assertThat(addComment(CLOSED, UserRole.ROLE_ADMIN, "{\"content\":\"too late\"}").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(getComments(CLOSED, UserRole.ROLE_ADMIN).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testBlankAndTooLongCommentsAreRejected() {
        Assertions.assertThat(addComment(OPEN_BUG, UserRole.ROLE_ADMIN, "{\"content\":\"   \"}").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(addComment(OPEN_BUG, UserRole.ROLE_ADMIN, "{\"content\":\"" + "x".repeat(5001) + "\"}").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUnknownRequest() {
        Assertions.assertThat(getComments("nope", UserRole.ROLE_ADMIN).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(addComment("nope", UserRole.ROLE_ADMIN, "{\"content\":\"hello\"}").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUsersCannotAccessRequestsOfOthers() {
        Assertions.assertThat(getComments(OPEN_BUG, UserRole.ROLE_USER).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(addComment(OPEN_BUG, UserRole.ROLE_USER, "{\"content\":\"hello\"}").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", statements = "UPDATE request SET participant_id = '4711' WHERE id = '1a2b3c4d-5678-90ab-cdef-1234567890ab'", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAuthorsCanCommentTheirOwnRequestAndAreShownByName() throws Exception {
        ResponseEntity<String> response = addComment(OPEN_BUG, UserRole.ROLE_USER, "{\"content\":\"still broken\"}");

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(objectMapper.readTree(response.getBody()).get("author").asText()).contains("Duck");

        JsonNode comments = objectMapper.readTree(getComments(OPEN_BUG, UserRole.ROLE_USER).getBody());
        Assertions.assertThat(comments.get(0).get("author").asText()).contains("Duck");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-requests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = { "DELETE FROM request_comment", "DELETE FROM request", "DELETE FROM participant" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testDeletingARequestRemovesItsComments() throws Exception {
        addComment(OPEN_BUG, UserRole.ROLE_ADMIN, "{\"content\":\"bye\"}");

        ResponseEntity<String> deleted = restTemplate.exchange(createURLWithPort("request/delete/" + OPEN_BUG), HttpMethod.DELETE, new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN)), String.class);

        Assertions.assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(getComments(OPEN_BUG, UserRole.ROLE_ADMIN).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
