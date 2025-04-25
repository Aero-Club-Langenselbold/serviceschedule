package de.ct.manager.schedule.settings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.ff4j.FF4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import de.ct.manager.schedule.plugins.FeatureConstants;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final CommonSettingsRepository commonSettingsRepository;
    private final FF4j ff4j;

    public SettingsService(SettingsRepository settingsRepository, CommonSettingsRepository commonSettingsRepository, FF4j ff4j) {
        this.settingsRepository = settingsRepository;
        this.commonSettingsRepository = commonSettingsRepository;
        this.ff4j = ff4j;
    }

    public SelectedState getSelectedState() {
        return settingsRepository.findAll().stream().findFirst().orElse(null);
    }

    public SelectedState saveSelectedState(SelectedState selectedState) {
        settingsRepository.deleteAll();

        Country country = settingsRepository.findCountryById(selectedState.getCountry().getId()).orElseThrow();
        State state = settingsRepository.findStateById(selectedState.getState().getId()).orElseThrow();

        selectedState.setCountry(country);
        selectedState.setState(state);

        return settingsRepository.save(selectedState);
    }

    public List<Country> findAllCountries() {
        return settingsRepository.findAllCountries().orElse(Collections.emptyList());
    }

    public List<State> findAllStatesByCountry(String countryId) {
        Country country = settingsRepository.findCountryById(countryId).orElseThrow();
        return settingsRepository.findAllStatesByCountry(country).orElse(Collections.emptyList());
    }

    public String getCommonSettingValue(String key) {
        Optional<CommonSettings> setting = commonSettingsRepository.findSettingByKey(key);

        if (setting.isPresent()) {
            return setting.get().getSettingValue();
        } else {
            return null;
        }
    }

    public List<CommonSettings> getAllCommonSettings() {
        if (!this.ff4j.check(FeatureConstants.SECURITY_QUESTION_FEATURE)) {
            return commonSettingsRepository.findAll(Sort.by("settingKey").descending()).stream()
                    .filter(commonSetting -> !commonSetting.getSettingKey().equals(SettingConstants.SECURITY_QUESTION)
                            && !commonSetting.getSettingKey().equals(SettingConstants.SECURITY_ANSWER))
                    .toList();
        } else {
            return commonSettingsRepository.findAll(Sort.by("settingKey").descending());
        }
    }

    public CommonSettings getCommonSetting(String key ) {
        return commonSettingsRepository.findSettingByKey(key).orElse(null);
    }

    public List<CommonSettings> saveCommonSettings(List<CommonSettings> commonSettings) {
        return commonSettingsRepository.saveAll(commonSettings);
    }

    public List<LocalDate> calculateHolidayDates(List<Holiday> holidays, Date dateFrom, Date dateTo) {
        if (dateFrom == null || dateTo == null || holidays == null || holidays.isEmpty() || dateFrom.after(dateTo)) {
            return Collections.emptyList();
        }

        List<LocalDate> holidayDates = new ArrayList<>();

        int yearFrom = dateFrom.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().getYear();
        int yearTo = dateTo.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().getYear();

        while (yearFrom <= yearTo) {
            LocalDate easter = calculateEaster(yearFrom);

            for (Holiday holiday : holidays) {
                LocalDate holidayDate = null;
                if (holiday.getDate() != null) {
                    // date is in format MM-DD
                    String[] dateParts = holiday.getDate().split("-");
                    int month = Integer.parseInt(dateParts[0]);
                    int day = Integer.parseInt(dateParts[1]);
                    holidayDate = LocalDate.of(yearFrom, month, day);
                } else if (holiday.getFormula() != null) {
                    switch (holiday.getFormula()) {
                        case EASTER_FRIDAY:
                            holidayDate = easter.minusDays(2);
                            break;
                        case EASTER_MONDAY:
                            holidayDate = easter.plusDays(1);
                            break;
                        case ASCENSION_DAY:
                            holidayDate = easter.plusDays(39);
                            break;
                        case CORPUS_CHRISTI:
                            holidayDate = easter.plusDays(60);
                            break;
                        case REPENTANCE_PRAYER:
                            LocalDate november23 = LocalDate.of(yearFrom, 11, 23);
                            holidayDate = november23.minusDays((november23.getDayOfWeek().getValue() + 4) % 7);
                            break;
                        case WHIT_MONDAY:
                            holidayDate = easter.plusDays(50);
                            break;
                        default:
                            break;
                    }
                }

                if (holidayDate != null && !holidayDate.isBefore(dateFrom.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                        && !holidayDate.isAfter(dateTo.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())) {
                    holidayDates.add(holidayDate);
                }
            }

            yearFrom++;
        }

        return holidayDates;
    }

    private LocalDate calculateEaster(int year) {
        int a = year % 19;
        int b = year % 4;
        int c = year % 7;
        int k = year / 100;
        int p = (13 + 8*k) / 25;
        int q = k / 4;
        int M = (15 - p + k - q) % 30;
        int N = (4 + k - q) % 7;
        int d = (19*a + M) % 30;
        int e = (2*b + 4*c + 6*d + N) % 7;

        if (d==29 && e == 6) {
            return LocalDate.of(year, 4, 19);
        } else if (d==28 && e==6 && ((11*M + 11)%30 < 10)) {
            return LocalDate.of(year, 4, 18);
        }

        int H = 22 + d + e;
        if (H <= 31) {
            return LocalDate.of(year, 3, H);
        }
        return LocalDate.of(year, 4, H-31);
    }
}
