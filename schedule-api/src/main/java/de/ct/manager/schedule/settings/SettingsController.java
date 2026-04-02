package de.ct.manager.schedule.settings;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.plan.Plan;
import de.ct.manager.schedule.plan.PlanService;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final PlanService planService;

    public SettingsController(SettingsService settingsService, PlanService planService) {
        this.settingsService = settingsService;
        this.planService = planService;
    }

    @GetMapping("/selected-state")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<SelectedState> getSelectedState() {
        return ResponseEntity.ok(settingsService.getSelectedState());
    }

    @GetMapping("/common-settings")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<CommonSettings>> getAllCommonSettings() {
        return ResponseEntity.ok(settingsService.getAllCommonSettings());
    }

    @GetMapping("/common-setting/{key}")
    public ResponseEntity<CommonSettings> getCommonSetting(@PathVariable String key) {
        return ResponseEntity.ok(settingsService.getCommonSetting(key));
    }

    @PostMapping("/common-settings")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<CommonSettings>> saveCommonSettings(@RequestBody List<CommonSettings> commonSettings) {
        return ResponseEntity.ok(settingsService.saveCommonSettings(commonSettings));
    }

    @PostMapping("/selected-state")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<SelectedState> saveSelectedState(@RequestBody SelectedState selectedState) {
        return ResponseEntity.ok(settingsService.saveSelectedState(selectedState));
    }

    @GetMapping("/countries")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<Iterable<Country>> findAllCountries() {
        return ResponseEntity.ok(settingsService.findAllCountries());
    }

    @GetMapping("/states/{countryId}")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<Iterable<State>> findAllStatesByCountry(@PathVariable String countryId) {
        return ResponseEntity.ok(settingsService.findAllStatesByCountry(countryId));
    }

    @GetMapping("/holidays/{planId}")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<Iterable<LocalDate>> calculateHolidayDates(@PathVariable String planId) {
        Plan plan = planService.getPlan(planId);
        if (plan == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Holiday> holidays = settingsService.getSelectedState().getState().getHolidays();

        return ResponseEntity.ok(settingsService.calculateHolidayDates(holidays, plan.getDateFrom(), plan.getDateUntil()));
    }

}
