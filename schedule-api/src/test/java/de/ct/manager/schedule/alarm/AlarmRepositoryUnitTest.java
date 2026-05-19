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
class AlarmRepositoryUnitTest {

    private static final LocalDateTime TIMESTAMP_1 = LocalDateTime.of(2026, 1, 1, 10, 0);
    private static final LocalDateTime TIMESTAMP_2 = LocalDateTime.of(2026, 1, 1, 11, 0);

    @Autowired
    AlarmRepository alarmRepository;

    @BeforeEach
    void setup() {
        Alarm alarm1 = new Alarm();
        alarm1.setTimestamp(TIMESTAMP_1);
        alarm1.setSystemTimestamp(TIMESTAMP_1);
        alarm1.setRfidId("rfid001");
        alarm1.setStatus(AlarmStatus.DISARMED);
        alarmRepository.save(alarm1);

        Alarm alarm2 = new Alarm();
        alarm2.setTimestamp(TIMESTAMP_2);
        alarm2.setSystemTimestamp(TIMESTAMP_2);
        alarm2.setRfidId("rfid002");
        alarm2.setStatus(AlarmStatus.ARMED);
        alarmRepository.save(alarm2);
    }

    @AfterEach
    void destroy() {
        alarmRepository.deleteAll();
    }

    @Test
    void existsBySystemTimestamp_returnsTrue_whenAlarmExists() {
        Assertions.assertThat(alarmRepository.existsBySystemTimestamp(TIMESTAMP_1)).isTrue();
    }

    @Test
    void existsBySystemTimestamp_returnsFalse_whenAlarmNotExists() {
        Assertions.assertThat(alarmRepository.existsBySystemTimestamp(LocalDateTime.of(2000, 1, 1, 0, 0))).isFalse();
    }

    @Test
    void findAll_returnsPaginatedResults() {
        Page<Alarm> page = alarmRepository.findAll(PageRequest.of(0, 1));
        Assertions.assertThat(page.getTotalElements()).isEqualTo(2);
        Assertions.assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findAll_withSpecification_filtersByRfidId() {
        Page<Alarm> page = alarmRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("rfidId"), "rfid001"),
                PageRequest.of(0, 10));
        Assertions.assertThat(page.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(page.getContent().get(0).getRfidId()).isEqualTo("rfid001");
    }
}
