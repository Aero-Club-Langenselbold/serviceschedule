package de.ct.manager.schedule.settings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class SettingsServiceUnitTest {

    @Mock
    SettingsRepository settingsRepository;

    @InjectMocks
    SettingsService settingsService;

    @Test
    void testGetSelectedState() {
        SelectedState selectedState = new SelectedState();
        selectedState.setCountry(new Country());
        selectedState.setState(new State());

        when(settingsRepository.findAll()).thenReturn(List.of(selectedState));

        SelectedState result = settingsService.getSelectedState();

        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void testSaveSelectedState() {
        SelectedState selectedState = new SelectedState();
        selectedState.setCountry(new Country());
        selectedState.setState(new State());

        when(settingsRepository.save(selectedState)).thenReturn(selectedState);
        when(settingsRepository.findCountryById(any())).thenReturn(Optional.of(new Country()));
        when(settingsRepository.findStateById(any())).thenReturn(Optional.of(new State()));

        selectedState = settingsService.saveSelectedState(selectedState);

        Assertions.assertThat(selectedState).isNotNull();
    }

    @Test
    void testFindAllCountries() {
        Country country = new Country();
        country.setName("Germany");

        when(settingsRepository.findAllCountries()).thenReturn(Optional.of(List.of(country)));

        List<Country> result = settingsService.findAllCountries();

        Assertions.assertThat(result).isNotNull().hasSize(1);
    }

    @Test
    void testFindAllStatesByCountry() {
        Country country = new Country();
        country.setName("Germany");

        State state = new State();
        state.setName("Bavaria");
        state.setCountry(country);

        when(settingsRepository.findCountryById(country.getId())).thenReturn(Optional.of(country));
        when(settingsRepository.findAllStatesByCountry(country)).thenReturn(Optional.of(List.of(state)));

        List<State> result = settingsService.findAllStatesByCountry(country.getId());

        Assertions.assertThat(result).isNotNull().hasSize(1);
    }

    @Test
    void testCalculateAnualHolidayDates() throws Exception {
        List<Holiday> holidays = generateHolidayList();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFrom = sdf.parse("2025-01-01");
        Date dateTo = sdf.parse("2025-12-31");

        List<LocalDate> result = settingsService.calculateHolidayDates(holidays, dateFrom, dateTo);
        Assertions.assertThat(result).isNotNull().hasSize(16);

        Assertions.assertThat(result).contains(LocalDate.parse("2025-01-06")); // 3 Kings
        Assertions.assertThat(result).contains(LocalDate.parse("2025-03-08")); // Womans Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-04-18")); // Good Friday
        Assertions.assertThat(result).contains(LocalDate.parse("2025-04-21")); // Easter Monday
        Assertions.assertThat(result).contains(LocalDate.parse("2025-05-01")); // Labour Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-05-29")); // Ascension Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-06-09")); // Whit Monday
        Assertions.assertThat(result).contains(LocalDate.parse("2025-06-19")); // Corpus Christi
        Assertions.assertThat(result).contains(LocalDate.parse("2025-08-15")); // Mariä Ascension
        Assertions.assertThat(result).contains(LocalDate.parse("2025-09-20")); // World childrens day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-10-03")); // German Unity Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-10-31")); // Reformation Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-11-01")); // All Saints Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-11-19")); // Day of Repentance and Prayer
        Assertions.assertThat(result).contains(LocalDate.parse("2025-12-25")); // Christmas Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-12-26")); // Boxing Day
    }

    @Test
    void testCalculatePartHolidayDates() throws Exception {
        List<Holiday> holidays = generateHolidayList();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFrom = sdf.parse("2025-01-01");
        Date dateTo = sdf.parse("2025-06-30");

        List<LocalDate> result = settingsService.calculateHolidayDates(holidays, dateFrom, dateTo);
        Assertions.assertThat(result).isNotNull().hasSize(8);

        Assertions.assertThat(result).containsOnly(LocalDate.parse("2025-01-06"), LocalDate.parse("2025-03-08"), LocalDate.parse("2025-04-18"),
                LocalDate.parse("2025-04-21"), LocalDate.parse("2025-05-01"), LocalDate.parse("2025-05-29"), LocalDate.parse("2025-06-09"),
                LocalDate.parse("2025-06-19"));
    }

    @Test
    void testCalculateTwoAnualHolidayDates() throws Exception {
        List<Holiday> holidays = generateHolidayList();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFrom = sdf.parse("2025-01-01");
        Date dateTo = sdf.parse("2026-12-31");

        List<LocalDate> result = settingsService.calculateHolidayDates(holidays, dateFrom, dateTo);
        Assertions.assertThat(result).isNotNull().hasSize(32);

        Assertions.assertThat(result).contains(LocalDate.parse("2025-01-06")); // 3 Kings
        Assertions.assertThat(result).contains(LocalDate.parse("2025-03-08")); // Womans Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-04-18")); // Good Friday
        Assertions.assertThat(result).contains(LocalDate.parse("2025-04-21")); // Easter Monday
        Assertions.assertThat(result).contains(LocalDate.parse("2025-05-01")); // Labour Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-05-29")); // Ascension Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-06-09")); // Whit Monday
        Assertions.assertThat(result).contains(LocalDate.parse("2025-06-19")); // Corpus Christi
        Assertions.assertThat(result).contains(LocalDate.parse("2025-08-15")); // Mariä Ascension
        Assertions.assertThat(result).contains(LocalDate.parse("2025-09-20")); // World childrens day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-10-03")); // German Unity Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-10-31")); // Reformation Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-11-01")); // All Saints Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-11-19")); // Day of Repentance and Prayer
        Assertions.assertThat(result).contains(LocalDate.parse("2025-12-25")); // Christmas Day
        Assertions.assertThat(result).contains(LocalDate.parse("2025-12-26")); // Boxing Day
        // only sample 2026
        Assertions.assertThat(result).contains(LocalDate.parse("2026-05-01")); // Labour Day
        Assertions.assertThat(result).contains(LocalDate.parse("2026-12-25")); // Christmas Day
        Assertions.assertThat(result).contains(LocalDate.parse("2026-12-26")); // Boxing Day

    }

    private List<Holiday> generateHolidayList() {
        List<Holiday> holidays = new ArrayList<>();

        holidays.add(new Holiday("3 Kings", "01-06", null));
        holidays.add(new Holiday("Womans Day", "03-08", null));
        holidays.add(new Holiday("Good Friday", null, Formula.EASTER_FRIDAY));
        holidays.add(new Holiday("Easter Monday", null, Formula.EASTER_MONDAY));
        holidays.add(new Holiday("Labour Day", "05-01", null));
        holidays.add(new Holiday("Ascension Day", null, Formula.ASCENSION_DAY));
        holidays.add(new Holiday("Whit Monday", null, Formula.WHIT_MONDAY));
        holidays.add(new Holiday("Corpus Christi", null, Formula.CORPUS_CHRISTI));
        holidays.add(new Holiday("Mariä Ascension", "08-15", null));
        holidays.add(new Holiday("World childrens day", "09-20", null));
        holidays.add(new Holiday("German Unity Day", "10-03", null));
        holidays.add(new Holiday("Reformation Day", "10-31", null));
        holidays.add(new Holiday("All Saints Day", "11-01", null));
        holidays.add(new Holiday("Day of Repentance and Prayer", null, Formula.REPENTANCE_PRAYER));
        holidays.add(new Holiday("Christmas Day", "12-25", null));
        holidays.add(new Holiday("Boxing Day", "12-26", null));

        return holidays;
    }
}
