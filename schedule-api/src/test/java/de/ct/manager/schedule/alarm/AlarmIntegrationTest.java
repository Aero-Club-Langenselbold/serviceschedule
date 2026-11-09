package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

class AlarmIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-alarms.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetAlarms_returnsSeededData() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("alarm/page"),
                HttpMethod.GET,
                entity,
                String.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-alarms.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetAlarms_filterByRfidId() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("alarm/page") + "?rfidId=rfid001",
                HttpMethod.GET,
                entity,
                String.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-alarms.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetAlarms_filterByStatus() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("alarm/page") + "?status=0",
                HttpMethod.GET,
                entity,
                String.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAddAlarm_success() {
        AlarmRequest request = new AlarmRequest();
        request.setTimestamp(LocalDateTime.of(2026, 3, 1, 10, 0));
        request.setRfidId("rfid-new");
        request.setInternalTime("10:00");
        request.setRawData("test raw");
        request.setStatus(AlarmStatus.AUTHORIZED_KEY);

        HttpEntity<AlarmRequest> entity = new HttpEntity<>(request, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));

        ResponseEntity<Alarm> response = restTemplate.exchange(
                createURLWithPort("alarm/save"),
                HttpMethod.POST,
                entity,
                Alarm.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getRfidId()).isEqualTo("rfid-new");
    }

    @Test
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAddAlarm_duplicate_returnsConflict() {
        AlarmRequest request = new AlarmRequest();
        request.setTimestamp(LocalDateTime.of(2026, 4, 1, 10, 0));
        request.setRfidId("rfid-dup");
        request.setStatus(AlarmStatus.UNAUTHORIZED_KEY);

        HttpEntity<AlarmRequest> entity = new HttpEntity<>(request, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));

        ResponseEntity<Alarm> first = restTemplate.exchange(
                createURLWithPort("alarm/save"), HttpMethod.POST, entity, Alarm.class);
        Assertions.assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = restTemplate.exchange(
                createURLWithPort("alarm/save"), HttpMethod.POST, entity, String.class);
        Assertions.assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void testGetAlarms_unauthorized_noToken() {
        HttpEntity<String> entity = new HttpEntity<>(null);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("alarm/page"),
                HttpMethod.GET,
                entity,
                String.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testGetAlarms_forbidden_wrongRole() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_USER));

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("alarm/page"),
                HttpMethod.GET,
                entity,
                String.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- AlarmDeviceStatus integration tests ---

    @Test
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAddDeviceStatus_success() {
        AlarmDeviceStatusRequest request = buildDeviceStatusRequest(LocalDateTime.of(2026, 5, 7, 22, 38, 45));

        HttpEntity<AlarmDeviceStatusRequest> entity = new HttpEntity<>(request, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));

        ResponseEntity<Alarm> response = restTemplate.exchange(
                createURLWithPort("alarm/status/save"),
                HttpMethod.POST,
                entity,
                Alarm.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(AlarmStatus.AUTHORIZED_KEY);
        Assertions.assertThat(response.getBody().isArmed()).isTrue();
    }

    @Test
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAddDeviceStatus_duplicate_returnsConflict() {
        AlarmDeviceStatusRequest request = buildDeviceStatusRequest(LocalDateTime.of(2026, 5, 8, 10, 0, 0));

        HttpEntity<AlarmDeviceStatusRequest> entity = new HttpEntity<>(request, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));

        ResponseEntity<Alarm> first = restTemplate.exchange(
                createURLWithPort("alarm/status/save"), HttpMethod.POST, entity, Alarm.class);
        Assertions.assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = restTemplate.exchange(
                createURLWithPort("alarm/status/save"), HttpMethod.POST, entity, String.class);
        Assertions.assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Sql(statements = "DELETE FROM alarm", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetAlarms_masksRfidButKeepsStoredValue() {
        AlarmRequest request = new AlarmRequest();
        request.setTimestamp(LocalDateTime.of(2026, 5, 9, 10, 0, 0));
        request.setRfidId("0123456789ABCDEF");
        request.setStatus(AlarmStatus.AUTHORIZED_KEY);
        restTemplate.exchange(
                createURLWithPort("alarm/save"),
                HttpMethod.POST,
                new HttpEntity<>(request, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN)),
                Alarm.class);

        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ALARM_ADMIN));
        String maskedRfid = "XXXXXXXXXXXBCDEF";

        for (int i = 0; i < 2; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    createURLWithPort("alarm/page"), HttpMethod.GET, entity, String.class);

            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(response.getBody()).contains(maskedRfid).doesNotContain("0123456789");
        }

        ResponseEntity<String> filtered = restTemplate.exchange(
                createURLWithPort("alarm/page") + "?rfidId=0123456789ABCDEF", HttpMethod.GET, entity, String.class);

        Assertions.assertThat(filtered.getBody()).contains(maskedRfid);
    }

    @Test
    @Sql(statements = {
            "DELETE FROM common_settings WHERE setting_key = 'alarm_password'",
            "INSERT INTO common_settings (id, version, setting_key, setting_value) VALUES ('alarm-password-test', 0, 'alarm_password', 'test-passphrase')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = {
            "DELETE FROM alarm",
            "DELETE FROM common_settings WHERE setting_key = 'alarm_password'"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAddDeviceStatus_withPassphrase_acceptsDevicePayload() {
        String payload = """
                {"timestamp":"2026-11-09T08:08:45.001Z","betrieb":1,"scharf":1,"unscharf":0,"sabotage":0,"stoerung":0,"alarm":0,\
                "zone1":0,"zone2":0,"zone3":0,"zone4":0,"zone5":0,"zone6":0,"zone7":0,"zone8":0}""";

        ResponseEntity<String> accepted = postDeviceStatus(payload, "test-passphrase");
        Assertions.assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> rejected = postDeviceStatus(payload, "wrong-passphrase");
        Assertions.assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> postDeviceStatus(String payload, String passphrase) {
        HttpHeaders headers = createHeadersWithToken(null);
        headers.set("X-Alarm-Passphrase", passphrase);
        return restTemplate.exchange(
                createURLWithPort("alarm/status/save"), HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
    }

    private AlarmDeviceStatusRequest buildDeviceStatusRequest(LocalDateTime timestamp) {
        AlarmDeviceStatusRequest request = new AlarmDeviceStatusRequest();
        request.setTimestamp(timestamp);
        request.setBetrieb(1);
        request.setScharf(1);
        request.setUnscharf(0);
        request.setSabotage(0);
        request.setStoerung(0);
        request.setAlarm(0);
        request.setZone1(0);
        request.setZone2(0);
        request.setZone3(1);
        request.setZone4(0);
        request.setZone5(1);
        request.setZone6(0);
        request.setZone7(0);
        request.setZone8(0);
        return request;
    }
}
