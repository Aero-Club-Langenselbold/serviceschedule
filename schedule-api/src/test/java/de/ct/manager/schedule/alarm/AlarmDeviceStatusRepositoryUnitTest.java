package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class AlarmDeviceStatusRepositoryUnitTest {

    private static final LocalDateTime TIMESTAMP_1 = LocalDateTime.of(2026, 5, 7, 22, 38, 45);
    private static final LocalDateTime TIMESTAMP_2 = LocalDateTime.of(2026, 5, 7, 23, 0, 0);

    @Autowired
    AlarmDeviceStatusRepository alarmDeviceStatusRepository;

    @BeforeEach
    void setup() {
        AlarmDeviceStatus status1 = buildStatus(TIMESTAMP_1, 1, 1, 0);
        alarmDeviceStatusRepository.save(status1);

        AlarmDeviceStatus status2 = buildStatus(TIMESTAMP_2, 1, 0, 1);
        alarmDeviceStatusRepository.save(status2);
    }

    @AfterEach
    void destroy() {
        alarmDeviceStatusRepository.deleteAll();
    }

    @Test
    void existsByTimestamp_returnsTrue_whenStatusExists() {
        Assertions.assertThat(alarmDeviceStatusRepository.existsByTimestamp(TIMESTAMP_1)).isTrue();
    }

    @Test
    void existsByTimestamp_returnsFalse_whenStatusNotExists() {
        Assertions.assertThat(alarmDeviceStatusRepository.existsByTimestamp(LocalDateTime.of(2000, 1, 1, 0, 0))).isFalse();
    }

    @Test
    void findAll_returnsPaginatedResults() {
        Page<AlarmDeviceStatus> page = alarmDeviceStatusRepository.findAll(PageRequest.of(0, 1));
        Assertions.assertThat(page.getTotalElements()).isEqualTo(2);
        Assertions.assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findAll_returnsAllResults() {
        Page<AlarmDeviceStatus> page = alarmDeviceStatusRepository.findAll(PageRequest.of(0, 10));
        Assertions.assertThat(page.getTotalElements()).isEqualTo(2);
    }

    private AlarmDeviceStatus buildStatus(LocalDateTime timestamp, int betrieb, int scharf, int unscharf) {
        AlarmDeviceStatus status = new AlarmDeviceStatus();
        status.setTimestamp(timestamp);
        status.setBetrieb(betrieb);
        status.setScharf(scharf);
        status.setUnscharf(unscharf);
        status.setSabotage(0);
        status.setStoerung(0);
        status.setAlarm(0);
        status.setZone1(0);
        status.setZone2(0);
        status.setZone3(1);
        status.setZone4(0);
        status.setZone5(1);
        status.setZone6(0);
        status.setZone7(0);
        status.setZone8(0);
        return status;
    }
}
