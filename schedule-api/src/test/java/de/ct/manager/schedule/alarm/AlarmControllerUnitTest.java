package de.ct.manager.schedule.alarm;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;
import de.ct.manager.schedule.util.TestUtils;

@WebMvcTest(AlarmController.class)
@Import({ AlarmService.class })
class AlarmControllerUnitTest extends AbstractControllerUnitTest {

    @MockitoBean
    private AlarmRepository alarmRepository;

    // Use Spring's fully-configured ObjectMapper (with JavaTimeModule) for request serialization
    @Autowired
    private ObjectMapper springObjectMapper;

    @BeforeEach
    void setup() {
        when(userRepository.findByUsername(TestUtils.TEST_ADMIN))
                .thenReturn(Optional.of(createTestUser(UserRole.ROLE_ALARM_ADMIN)));
        when(userRepository.findByUsername(TestUtils.TEST_USER))
                .thenReturn(Optional.of(createTestUser(UserRole.ROLE_USER)));
    }

    @Test
    void testAddAlarm_success() throws Exception {
        AlarmRequest request = buildRequest(LocalDateTime.of(2026, 1, 1, 10, 0));
        Alarm alarm = buildAlarm("alarm-1", LocalDateTime.of(2026, 1, 1, 10, 0));

        when(alarmRepository.existsByTimestamp(any())).thenReturn(false);
        when(alarmRepository.save(any(Alarm.class))).thenReturn(alarm);

        mockMvc.perform(post("/alarm/save")
                        .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ALARM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(springObjectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rfidId", is("rfid001")));
    }

    @Test
    void testAddAlarm_duplicate_returnsConflict() throws Exception {
        AlarmRequest request = buildRequest(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(alarmRepository.existsByTimestamp(any())).thenReturn(true);

        mockMvc.perform(post("/alarm/save")
                        .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ALARM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(springObjectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAlarms_success() throws Exception {
        Alarm alarm = buildAlarm("alarm-1", LocalDateTime.of(2026, 1, 1, 10, 0));
        Page<Alarm> page = new PageImpl<>(List.of(alarm));

        when(alarmRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/alarm/page")
                        .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ALARM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rfidId", is("rfid001")));
    }

    @Test
    void testGetAlarms_unauthorized_noToken() throws Exception {
        mockMvc.perform(get("/alarm/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAlarms_forbidden_wrongRole() throws Exception {
        mockMvc.perform(get("/alarm/page")
                        .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
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

    private Alarm buildAlarm(String id, LocalDateTime timestamp) {
        Alarm alarm = new Alarm();
        alarm.setId(id);
        alarm.setTimestamp(timestamp);
        alarm.setRfidId("rfid001");
        alarm.setInternalTime("10:00");
        alarm.setRawData("raw data");
        alarm.setStatus(AlarmStatus.ARMED);
        return alarm;
    }
}
