package de.ct.manager.schedule.settings;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class SettingsRepositoryUnitTest {

    @Autowired
    SettingsRepository settingsRepository;

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findAllCountries() {
        Optional<List<Country>> countries = settingsRepository.findAllCountries();
        Assertions.assertThat(countries).isNotEmpty();
        Assertions.assertThat(countries.get().size()).isEqualTo(1);
        Assertions.assertThat(countries.get().get(0).getName()).isEqualTo("Germany");
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findStateByCountry() {
        Optional<List<Country>> countries = settingsRepository.findAllCountries();
        Assertions.assertThat(countries).isNotEmpty();
        Assertions.assertThat(countries.get().size()).isEqualTo(1);

        Country country = countries.get().get(0);

        Optional<List<State>> states = settingsRepository.findAllStatesByCountry(country);
        Assertions.assertThat(states).isNotEmpty();
        Assertions.assertThat(states.get().size()).isEqualTo(16);

        State state = states.get().get(0);
        Assertions.assertThat(state.getHolidays()).isNotEmpty();
        Assertions.assertThat(state.getHolidays().size()).isGreaterThan(0);
    }

    @Test
    @Sql(scripts = "classpath:scripts/test-holidays.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:scripts/delete-holidays.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void canHandleSelectedState() {
        Optional<List<Country>> countries = settingsRepository.findAllCountries();
        Assertions.assertThat(countries).isNotEmpty();
        Assertions.assertThat(countries.get().size()).isEqualTo(1);

        Country country = countries.get().get(0);

        Optional<List<State>> states = settingsRepository.findAllStatesByCountry(country);
        Assertions.assertThat(states).isNotEmpty();
        Assertions.assertThat(states.get().size()).isEqualTo(16);

        State state = states.get().get(0);
        Assertions.assertThat(state.getHolidays()).isNotEmpty();
        Assertions.assertThat(state.getHolidays().size()).isGreaterThan(0);

        SelectedState selectedState = new SelectedState();
        selectedState.setState(state);
        selectedState.setCountry(country);
        selectedState = settingsRepository.save(selectedState);

        Assertions.assertThat(selectedState.getId()).isNotNull();

        settingsRepository.delete(selectedState);
    }
}
