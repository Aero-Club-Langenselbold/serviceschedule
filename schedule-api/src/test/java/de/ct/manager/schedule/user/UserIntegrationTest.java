package de.ct.manager.schedule.user;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
class UserIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_USER_ID = "1234";
    private static final String ADMIN_ID = "5678";

    private HttpHeaders headersWithToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<String> call(HttpMethod method, String path, HttpHeaders headers, String body) {
        return restTemplate.exchange(createURLWithPort(path), method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode impersonateTestUser() throws Exception {
        ResponseEntity<String> response = call(HttpMethod.POST, "user/" + TEST_USER_ID + "/impersonate", createHeadersWithToken(UserRole.ROLE_ADMIN), null);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    @Test
    void testUserPageListsRegisteredUsers() throws Exception {
        ResponseEntity<String> response = call(HttpMethod.GET, "user/page", createHeadersWithToken(UserRole.ROLE_ADMIN), null);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = objectMapper.readTree(response.getBody()).get("content");
        Assertions.assertThat(content).hasSize(2);
        Assertions.assertThat(content.get(0).get("username").asText()).isEqualTo("admin@test.de");
        Assertions.assertThat(content.get(0).has("password")).isFalse();
        // the UI reads the total for its pagination from here
        Assertions.assertThat(objectMapper.readTree(response.getBody()).at("/totalElements").asInt()).isEqualTo(2);
    }

    @Test
    @Sql(statements = "INSERT INTO participant (id, version, firstname, lastname, email, status, date_of_birth, max_assignments, min_assignments) VALUES ('4711', 0, 'Donald', 'Duck', 'test@test.com', 'ACTIVE', '1934-06-09', 0, 0)", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUserPageSearchesUsernameAndParticipantName() throws Exception {
        ResponseEntity<String> byName = call(HttpMethod.GET, "user/page?search=duck", createHeadersWithToken(UserRole.ROLE_ADMIN), null);
        JsonNode content = objectMapper.readTree(byName.getBody()).get("content");
        Assertions.assertThat(content).hasSize(1);
        Assertions.assertThat(content.get(0).get("username").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(content.get(0).get("fullname").asText()).contains("Duck");

        ResponseEntity<String> byMail = call(HttpMethod.GET, "user/page?search=admin@", createHeadersWithToken(UserRole.ROLE_ADMIN), null);
        Assertions.assertThat(objectMapper.readTree(byMail.getBody()).get("content")).hasSize(1);
    }

    @Test
    @Sql(statements = "DELETE FROM auth_user_permissions", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGrantedPermissionsUnlockTheModuleAndRevokingLocksIt() throws Exception {
        HttpHeaders admin = createHeadersWithToken(UserRole.ROLE_ADMIN);
        HttpHeaders user = createHeadersWithToken(UserRole.ROLE_USER);

        Assertions.assertThat(call(HttpMethod.GET, "service/list", user, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.GET, "alarm/page", user, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // several rights at once
        ResponseEntity<String> granted = call(HttpMethod.PUT, "user/" + TEST_USER_ID + "/permissions", admin, "[\"PERMISSION_SCHEDULE_MANAGE\",\"PERMISSION_ALARM_MANAGE\"]");
        Assertions.assertThat(granted.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(objectMapper.readTree(granted.getBody()).get("permissions")).hasSize(2);
        Assertions.assertThat(call(HttpMethod.GET, "service/list", user, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(call(HttpMethod.GET, "alarm/page", user, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        // a right never opens up the administration of users
        Assertions.assertThat(call(HttpMethod.GET, "user/page", user, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // revoke one of them
        call(HttpMethod.PUT, "user/" + TEST_USER_ID + "/permissions", admin, "[\"PERMISSION_ALARM_MANAGE\"]");
        Assertions.assertThat(call(HttpMethod.GET, "service/list", user, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(call(HttpMethod.GET, "alarm/page", user, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testUpdatingPermissionsDoesNotTouchRoles() throws Exception {
        HttpHeaders admin = createHeadersWithToken(UserRole.ROLE_ADMIN);

        ResponseEntity<String> response = call(HttpMethod.PUT, "user/" + ADMIN_ID + "/permissions", admin, "[]");

        Assertions.assertThat(objectMapper.readTree(response.getBody()).get("roles")).hasSize(2);
    }

    @Test
    void testImpersonationActsWithTheRightsOfTheTargetAndCanBeLeft() throws Exception {
        JsonNode impersonation = impersonateTestUser();
        Assertions.assertThat(impersonation.get("username").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(impersonation.get("impersonator").asText()).isEqualTo("admin@test.de");

        // the admin rights are gone while impersonating
        HttpHeaders impersonated = headersWithToken(impersonation.get("token").asText());
        Assertions.assertThat(call(HttpMethod.GET, "user/page", impersonated, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> stopped = call(HttpMethod.POST, "user/impersonation/stop", impersonated, null);
        Assertions.assertThat(stopped.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode session = objectMapper.readTree(stopped.getBody());
        Assertions.assertThat(session.get("username").asText()).isEqualTo("admin@test.de");
        Assertions.assertThat(session.get("impersonator").isNull()).isTrue();

        // the new token is a regular admin token again
        HttpHeaders admin = headersWithToken(session.get("token").asText());
        Assertions.assertThat(call(HttpMethod.GET, "user/page", admin, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(call(HttpMethod.POST, "user/impersonation/stop", admin, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testCannotImpersonateYourselfOrUnknownUsers() {
        HttpHeaders admin = createHeadersWithToken(UserRole.ROLE_ADMIN);

        Assertions.assertThat(call(HttpMethod.POST, "user/" + ADMIN_ID + "/impersonate", admin, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(call(HttpMethod.POST, "user/nope/impersonate", admin, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
