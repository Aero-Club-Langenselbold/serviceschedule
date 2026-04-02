package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
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
        request.setStatus(AlarmStatus.ARMED);

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
        request.setStatus(AlarmStatus.DISARMED);

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
}
