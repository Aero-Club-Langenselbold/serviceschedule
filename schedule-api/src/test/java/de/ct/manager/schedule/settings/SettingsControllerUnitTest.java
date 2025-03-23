package de.ct.manager.schedule.settings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.ct.manager.schedule.participant.DateParticipantAvailabilityRepository;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.plan.Plan;
import de.ct.manager.schedule.plan.PlanRepository;
import de.ct.manager.schedule.plan.PlanService;
import de.ct.manager.schedule.service.ServiceDateRepository;
import de.ct.manager.schedule.service.ServiceRepository;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(SettingsController.class)
@Import({
    SettingsService.class,
    PlanService.class
})
public class SettingsControllerUnitTest extends AbstractControllerUnitTest {

    @MockitoBean
    private SettingsRepository settingsRepository;

    @MockitoBean
    private PlanRepository planRepository;

    @MockitoBean
    private ParticipantRepository participantRepository;

    @MockitoBean
    private ServiceDateRepository serviceDateRepository;

    @MockitoBean
    private DateParticipantAvailabilityRepository dateParticipantAvailabilityRepository;

    @MockitoBean
    private ServiceRepository serviceRepository;

    @Test
    void testGetSelectedState() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        SelectedState selectedState = new SelectedState();
        selectedState.setCountry(new Country());
        selectedState.setState(new State());
        when(settingsRepository.findAll()).thenReturn(List.of(selectedState));

        mockMvc.perform(
            get("/settings/selected-state")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testSaveSelectedState() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        SelectedState selectedState = new SelectedState();
        selectedState.setId("1");
        selectedState.setCountry(new Country());
        selectedState.setState(new State());
        when(settingsRepository.findCountryById(any())).thenReturn(Optional.of(new Country()));
        when(settingsRepository.findStateById(any())).thenReturn(Optional.of(new State()));
        when(settingsRepository.save(any())).thenReturn(selectedState);

        mockMvc.perform(
            post("/settings/selected-state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selectedState))
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testFindAllCountries() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        Country country = new Country();
        country.setName("Germany");

        when(settingsRepository.findAllCountries()).thenReturn(Optional.of(List.of(country)));

        mockMvc.perform(
            get("/settings/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name", is("Germany")));
    }

    @Test
    void testFindAllStatesByCountry() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        State state = new State();
        state.setName("Bavaria");

        when(settingsRepository.findCountryById("1")).thenReturn(Optional.of(new Country()));
        when(settingsRepository.findAllStatesByCountry(any())).thenReturn(Optional.of(List.of(state)));

        mockMvc.perform(
            get("/settings/states/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name", is("Bavaria")));
    }

    @Test
    void testCalculateHolidayDates() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFrom = sdf.parse("2025-01-01");
        Date dateTo = sdf.parse("2025-12-31");

        Plan plan = new Plan();
        plan.setId("1");
        plan.setDateFrom(dateFrom);
        plan.setDateUntil(dateTo);

        Holiday holiday = new Holiday();
        holiday.setDate("12-24");
        holiday.setName("Not Chrismtas");

        State state = new State();
        state.setHolidays(List.of(holiday));
        state.setName("Chinatown");

        SelectedState selectedState = new SelectedState();
        selectedState.setState(state);

        when(planRepository.findById("1")).thenReturn(Optional.of(plan));
        when(settingsRepository.findAll()).thenReturn(List.of(selectedState));

        mockMvc.perform(
            get("/settings/holidays/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0]", is("2025-12-24")));
    }
}
