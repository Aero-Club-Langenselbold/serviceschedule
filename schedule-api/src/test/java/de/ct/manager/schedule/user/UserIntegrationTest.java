package de.ct.manager.schedule.user;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
class UserIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_USER_ID = "1234";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

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

        Assertions.assertThat(objectMapper.readTree(response.getBody()).get("roles")).hasSize(1);
    }

    private static final String RESTORE_ROLES_1 = "DELETE FROM auth_user_roles";
    private static final String RESTORE_ROLES_2 = "INSERT INTO auth_user_roles (user_id, role_id) VALUES "
            + "('1234', '00000000-0000-0000-0000-000000000001'), ('5678', '00000000-0000-0000-0000-000000000002')";

    @Test
    void testAdministratorsSeeTheAssignableRoles() throws Exception {
        ResponseEntity<String> response = call(HttpMethod.GET, "user/roles", createHeadersWithToken(UserRole.ROLE_ADMIN), null);

        Assertions.assertThat(objectMapper.readValue(response.getBody(), String[].class))
                .containsExactly("ROLE_ADMIN");
        Assertions.assertThat(call(HttpMethod.GET, "user/roles", createHeadersWithToken(UserRole.ROLE_USER), null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Sql(statements = { RESTORE_ROLES_1, RESTORE_ROLES_2 }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAdministratorsGrantAndRevokeTheAdministratorRole() throws Exception {
        HttpHeaders admin = createHeadersWithToken(UserRole.ROLE_ADMIN);
        HttpHeaders user = createHeadersWithToken(UserRole.ROLE_USER);
        Assertions.assertThat(call(HttpMethod.GET, "user/page", user, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> granted = call(HttpMethod.PUT, "user/" + TEST_USER_ID + "/roles", admin, "[\"ROLE_ADMIN\"]");
        Assertions.assertThat(granted.getStatusCode()).isEqualTo(HttpStatus.OK);
        // the base role stays
        Assertions.assertThat(objectMapper.readValue(objectMapper.readTree(granted.getBody()).get("roles").toString(), String[].class))
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
        // effective right away, the roles are read from the database on every request
        Assertions.assertThat(call(HttpMethod.GET, "user/page", user, null).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> revoked = call(HttpMethod.PUT, "user/" + TEST_USER_ID + "/roles", admin, "[]");
        Assertions.assertThat(objectMapper.readValue(objectMapper.readTree(revoked.getBody()).get("roles").toString(), String[].class))
                .containsExactly("ROLE_USER");
        Assertions.assertThat(call(HttpMethod.GET, "user/page", user, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testThereAreNoRolesBesidesAdministratorAndUser() throws Exception {
        HttpHeaders admin = createHeadersWithToken(UserRole.ROLE_ADMIN);

        Assertions.assertThat(UserRole.values()).containsExactly(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);
        // the former module roles are no roles anymore, the rights replace them
        Assertions.assertThat(call(HttpMethod.PUT, "user/" + TEST_USER_ID + "/roles", admin, "[\"ROLE_MAINTENANCE_ADMIN\"]").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testTheOperationsRightCanBeGranted() throws Exception {
        ResponseEntity<String> response = call(HttpMethod.GET, "user/permissions", createHeadersWithToken(UserRole.ROLE_ADMIN), null);

        Assertions.assertThat(objectMapper.readValue(response.getBody(), String[].class))
                .containsExactly("PERMISSION_SCHEDULE_MANAGE", "PERMISSION_ALARM_MANAGE", "PERMISSION_MAINTENANCE_MANAGE", "PERMISSION_OPERATIONS_MANAGE");
    }

    @Test
    @Sql(statements = { RESTORE_ROLES_1, RESTORE_ROLES_2 }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAdministratorsCannotRevokeTheirOwnAdministratorRole() throws Exception {
        HttpHeaders admin = createHeadersWithToken(UserRole.ROLE_ADMIN);

        Assertions.assertThat(call(HttpMethod.PUT, "user/" + ADMIN_ID + "/roles", admin, "[]").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<String> changed = call(HttpMethod.PUT, "user/" + ADMIN_ID + "/roles", admin, "[\"ROLE_ADMIN\"]");
        Assertions.assertThat(objectMapper.readValue(objectMapper.readTree(changed.getBody()).get("roles").toString(), String[].class))
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void testOnlyAdministratorsChangeRoles() {
        ResponseEntity<String> response = call(HttpMethod.PUT, "user/" + TEST_USER_ID + "/roles", createHeadersWithToken(UserRole.ROLE_USER), "[\"ROLE_ADMIN\"]");

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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

    // --- personal settings: kiosk PIN

    @Test
    @Sql(statements = "UPDATE auth_user SET kiosk_pin = NULL", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUsersSetTheirKioskPinWhichIsStoredHashed() throws Exception {
        HttpHeaders user = createHeadersWithToken(UserRole.ROLE_USER);

        JsonNode before = objectMapper.readTree(call(HttpMethod.GET, "user/me/settings", user, null).getBody());
        Assertions.assertThat(before.get("username").asText()).isEqualTo("test@test.com");
        Assertions.assertThat(before.get("kioskPinSet").asBoolean()).isFalse();

        ResponseEntity<String> set = call(HttpMethod.PUT, "user/me/kiosk-pin", user, "{\"pin\":\"012345\"}");
        Assertions.assertThat(set.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(set.getBody()).doesNotContain("012345");
        Assertions.assertThat(objectMapper.readTree(set.getBody()).get("kioskPinSet").asBoolean()).isTrue();

        String stored = jdbcTemplate.queryForObject("SELECT kiosk_pin FROM auth_user WHERE id = ?", String.class, TEST_USER_ID);
        Assertions.assertThat(stored).isNotEqualTo("012345").startsWith("$2");
        Assertions.assertThat(passwordEncoder.matches("012345", stored)).isTrue();
        Assertions.assertThat(userService.matchesKioskPin(userRepository.findById(TEST_USER_ID).orElseThrow(), "012345")).isTrue();
        Assertions.assertThat(userService.matchesKioskPin(userRepository.findById(TEST_USER_ID).orElseThrow(), "543210")).isFalse();
        // only the own PIN is changed
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT kiosk_pin FROM auth_user WHERE id = ?", String.class, ADMIN_ID)).isNull();
        // the PIN never shows up anywhere, not even hashed
        Assertions.assertThat(call(HttpMethod.GET, "user/me/settings", user, null).getBody()).doesNotContain(stored).doesNotContain("012345");
        Assertions.assertThat(call(HttpMethod.GET, "user/page", createHeadersWithToken(UserRole.ROLE_ADMIN), null).getBody()).doesNotContain("kiosk");

        ResponseEntity<String> removed = call(HttpMethod.DELETE, "user/me/kiosk-pin", user, null);
        Assertions.assertThat(objectMapper.readTree(removed.getBody()).get("kioskPinSet").asBoolean()).isFalse();
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT kiosk_pin FROM auth_user WHERE id = ?", String.class, TEST_USER_ID)).isNull();
    }

    @Test
    void testTheKioskPinHasSixDigits() throws Exception {
        HttpHeaders user = createHeadersWithToken(UserRole.ROLE_USER);

        for (String pin : new String[] { "12345", "1234567", "12345a", "", " 123456" }) {
            Assertions.assertThat(call(HttpMethod.PUT, "user/me/kiosk-pin", user, "{\"pin\":\"" + pin + "\"}").getStatusCode())
                    .as(pin).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        Assertions.assertThat(call(HttpMethod.PUT, "user/me/kiosk-pin", user, "{}").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT kiosk_pin FROM auth_user WHERE id = ?", String.class, TEST_USER_ID)).isNull();
    }

    @Test
    void testSettingsNeedASignedInUser() {
        Assertions.assertThat(call(HttpMethod.GET, "user/me/settings", createHeadersWithToken(null), null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertThat(call(HttpMethod.PUT, "user/me/kiosk-pin", createHeadersWithToken(null), "{\"pin\":\"123456\"}").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
