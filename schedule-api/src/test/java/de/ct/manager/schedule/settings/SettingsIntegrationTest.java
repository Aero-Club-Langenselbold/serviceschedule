package de.ct.manager.schedule.settings;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
public class SettingsIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetCountryList() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<List<Country>> response = restTemplate.exchange(
            createURLWithPort("settings/") + "countries",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Country>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).isNotEmpty();
        Assertions.assertThat(response.getBody()).hasSize(1);
        Assertions.assertThat(response.getBody().get(0).getName()).isEqualTo("Germany");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetStateList() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<List<State>> response = restTemplate.exchange(
            createURLWithPort("settings/") + "states/f47ac10b-58cc-4372-a567-0e02b2c3d479",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<State>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).isNotEmpty();
        Assertions.assertThat(response.getBody()).hasSize(16);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetHolidayDates() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<List<LocalDate>> response = restTemplate.exchange(
            createURLWithPort("settings/") + "holidays/6000",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<LocalDate>>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).isNotEmpty();
        Assertions.assertThat(response.getBody()).hasSize(10);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetSelectedState() {
        HttpEntity<String> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<SelectedState> response = restTemplate.exchange(
            createURLWithPort("settings/") + "selected-state",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<SelectedState>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getCountry().getName()).isEqualTo("Germany");
        Assertions.assertThat(response.getBody().getState().getName()).isEqualTo("Hesse");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testSaveSelectedState() {
        // get countries
        HttpEntity<SelectedState> entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<List<Country>> countries = restTemplate.exchange(
            createURLWithPort("settings/") + "countries",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Country>>() {});

        // get states
        ResponseEntity<List<State>> states = restTemplate.exchange(
            createURLWithPort("settings/") + "states/" + countries.getBody().get(0).getId(),
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<State>>() {});

        // create selected state
        SelectedState selectedState = new SelectedState();
        selectedState.setCountry(countries.getBody().get(0));
        selectedState.setState(states.getBody().stream()
            .filter(state -> "Bavaria".equals(state.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("State 'Bavaria' not found")));

        // save selected state
        entity = new HttpEntity<>(selectedState, createHeadersWithToken(UserRole.ROLE_ADMIN));

        ResponseEntity<SelectedState> response = restTemplate.exchange(
            createURLWithPort("settings/") + "selected-state",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<SelectedState>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getCountry().getName()).isEqualTo("Germany");
        Assertions.assertThat(response.getBody().getState().getName()).isEqualTo("Bavaria");

        // get selected state
        entity = new HttpEntity<>(null, createHeadersWithToken(UserRole.ROLE_ADMIN));

        response = restTemplate.exchange(
            createURLWithPort("settings/") + "selected-state",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<SelectedState>() {});

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getCountry().getName()).isEqualTo("Germany");
        Assertions.assertThat(response.getBody().getState().getName()).isEqualTo("Bavaria");
    }
}
