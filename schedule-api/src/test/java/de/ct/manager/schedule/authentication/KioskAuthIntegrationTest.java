package de.ct.manager.schedule.authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.assertj.core.api.Assertions;
import org.ff4j.FF4j;
import org.ff4j.core.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import com.fasterxml.jackson.databind.JsonNode;

import de.ct.manager.schedule.plugins.FeatureConstants;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
class KioskAuthIntegrationTest extends AbstractIntegrationTest {

    private static final String USER = "test@test.com";
    private static final String ADMIN = "admin@test.de";
    private static final String PIN = "123456";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KioskAuthService kioskAuthService;

    @Autowired
    private FF4j ff4j;

    @BeforeEach
    void setPins() {
        kioskAuthService.resetAttempts();
        kioskAuthService.setClock(Clock.systemUTC());
        jdbcTemplate.update("UPDATE auth_user SET kiosk_pin = ? WHERE username IN (?, ?)", passwordEncoder.encode(PIN), USER, ADMIN);
    }

    @AfterEach
    void cleanUp() {
        kioskAuthService.resetAttempts();
        kioskAuthService.setClock(Clock.systemUTC());
        jdbcTemplate.update("UPDATE auth_user SET kiosk_pin = NULL");
        for (String feature : new String[] { FeatureConstants.VF_LOGIN_FEATURE, FeatureConstants.SECURITY_QUESTION_FEATURE }) {
            if (ff4j.exist(feature)) {
                ff4j.disable(feature);
            }
        }
    }

    private ResponseEntity<String> signIn(String username, String pin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"" + username + "\",\"pin\":\"" + pin + "\"}";
        return restTemplate.exchange(createURLWithPort("auth/kiosk/signin"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private String kioskToken(String username) throws Exception {
        ResponseEntity<String> response = signIn(username, PIN);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody()).get("token").asText();
    }

    private HttpStatus get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return HttpStatus.valueOf(restTemplate.exchange(createURLWithPort(path), HttpMethod.GET, new HttpEntity<>(headers), String.class)
                .getStatusCode().value());
    }

    @Test
    void testSignsInWithNameAndPinAsKioskMember() throws Exception {
        ResponseEntity<String> response = signIn(USER, PIN);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        Assertions.assertThat(body.get("username").asText()).isEqualTo(USER);
        Assertions.assertThat(body.get("kiosk").asBoolean()).isTrue();
        Assertions.assertThat(body.get("roles")).extracting(JsonNode::asText).containsExactly("ROLE_USER");
        Assertions.assertThat(body.get("token").asText()).isNotBlank();
    }

    @Test
    void testIgnoresTheRightsOfAdministratorsInTheKioskMode() throws Exception {
        JsonNode body = objectMapper.readTree(signIn(ADMIN, PIN).getBody());
        Assertions.assertThat(body.get("roles")).extracting(JsonNode::asText).containsExactly("ROLE_USER");
        Assertions.assertThat(body.get("permissions")).isEmpty();

        String token = body.get("token").asText();
        // plain member pages work
        Assertions.assertThat(get("maintenance/aircraft", token)).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(get("maintenance/defects", token)).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(get("operations/overview", token)).isEqualTo(HttpStatus.OK);
        // administration does not, although the user is an administrator
        Assertions.assertThat(get("maintenance/defects/assignable-users", token)).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(get("operations/checklists", token)).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(get("user/page", token)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testKioskSessionsReachOnlyMaintenanceAndOperations() throws Exception {
        String token = kioskToken(USER);

        // bug list, personal settings, notifications and the service schedules are closed
        Assertions.assertThat(get("request/list", token)).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(get("user/me/settings", token)).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(get("notifications", token)).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(get("plan/mine", token)).isEqualTo(HttpStatus.FORBIDDEN);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        ResponseEntity<String> pin = restTemplate.exchange(createURLWithPort("user/me/kiosk-pin"), HttpMethod.PUT,
                new HttpEntity<>("{\"pin\":\"654321\"}", headers), String.class);
        Assertions.assertThat(pin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // public endpoints stay open
        Assertions.assertThat(get("featureflag/active", token)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testRegularSessionsAreNotLimited() {
        HttpHeaders headers = createHeadersWithToken(de.ct.manager.schedule.user.UserRole.ROLE_USER);
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("user/me/settings"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testRejectsWrongPinsUnknownUsersAndUsersWithoutPin() {
        Assertions.assertThat(signIn(USER, "654321").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertThat(signIn("nobody@test.com", PIN).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        jdbcTemplate.update("UPDATE auth_user SET kiosk_pin = NULL WHERE username = ?", USER);
        Assertions.assertThat(signIn(USER, PIN).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testValidatesTheRequest() {
        Assertions.assertThat(signIn("", PIN).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(signIn(USER, "").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testDoesNotNeedTheSecurityAnswerOrTheVereinsfliegerLogin() {
        for (String feature : new String[] { FeatureConstants.VF_LOGIN_FEATURE, FeatureConstants.SECURITY_QUESTION_FEATURE }) {
            if (ff4j.exist(feature)) {
                ff4j.enable(feature);
            } else {
                ff4j.createFeature(new Feature(feature, true));
            }
        }

        Assertions.assertThat(signIn(USER, PIN).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testLocksTheKioskSignInAfterRepeatedFailures() {
        Instant start = Instant.parse("2026-11-27T10:00:00Z");
        kioskAuthService.setClock(Clock.fixed(start, ZoneOffset.UTC));

        for (int i = 0; i < KioskAuthService.MAX_FAILED_ATTEMPTS; i++) {
            Assertions.assertThat(signIn(USER, "000000").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        // even the right PIN is refused while locked, other users are not affected
        Assertions.assertThat(signIn(USER, PIN).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        Assertions.assertThat(signIn(USER.toUpperCase(), PIN).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        Assertions.assertThat(signIn(ADMIN, PIN).getStatusCode()).isEqualTo(HttpStatus.OK);

        kioskAuthService.setClock(Clock.fixed(start.plus(KioskAuthService.LOCK_DURATION).plusSeconds(1), ZoneOffset.UTC));
        Assertions.assertThat(signIn(USER, PIN).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testSuccessfulSignInResetsTheFailures() {
        for (int i = 0; i < KioskAuthService.MAX_FAILED_ATTEMPTS - 1; i++) {
            signIn(USER, "000000");
        }
        Assertions.assertThat(signIn(USER, PIN).getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(signIn(USER, "000000").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertThat(signIn(USER, PIN).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
