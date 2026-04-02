package de.ct.manager.schedule.alarm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AlarmServiceUnitTest {

    @Mock
    AlarmRepository alarmRepository;

    @InjectMocks
    AlarmService alarmService;

    @Test
    void testAddAlarm_success() {
        AlarmRequest request = buildRequest(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(alarmRepository.existsByTimestamp(any())).thenReturn(false);
        when(alarmRepository.save(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        Alarm result = alarmService.addAlarm(request);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getTimestamp()).isEqualTo(request.getTimestamp());
        Assertions.assertThat(result.getRfidId()).isEqualTo("rfid001");
        Assertions.assertThat(result.getStatus()).isEqualTo(AlarmStatus.ARMED);
        verify(alarmRepository).save(any(Alarm.class));
    }

    @Test
    void testAddAlarm_duplicate_throwsException() {
        AlarmRequest request = buildRequest(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(alarmRepository.existsByTimestamp(any())).thenReturn(true);

        Assertions.assertThatThrownBy(() -> alarmService.addAlarm(request))
                .isInstanceOf(AlarmDuplicateException.class);
        verify(alarmRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAlarms_returnsPage() {
        Alarm alarm = new Alarm();
        alarm.setTimestamp(LocalDateTime.of(2026, 1, 1, 10, 0));
        Page<Alarm> expected = new PageImpl<>(List.of(alarm));
        Pageable pageable = PageRequest.of(0, 10);
        when(alarmRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expected);

        Page<Alarm> result = alarmService.getAlarms(null, null, null, null, pageable);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAlarms_withFilters() {
        Page<Alarm> expected = new PageImpl<>(List.of());
        Pageable pageable = PageRequest.of(0, 10);
        when(alarmRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expected);

        Page<Alarm> result = alarmService.getAlarms("rfid001", AlarmStatus.ARMED,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 2, 0, 0), pageable);

        Assertions.assertThat(result).isNotNull();
        verify(alarmRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private AlarmRequest buildRequest(LocalDateTime timestamp) {
        AlarmRequest request = new AlarmRequest();
        request.setTimestamp(timestamp);
        request.setRfidId("rfid001");
        request.setInternalTime("10:00");
        request.setRawData("raw data");
        request.setStatus(AlarmStatus.ARMED);
        return request;
    }
}
