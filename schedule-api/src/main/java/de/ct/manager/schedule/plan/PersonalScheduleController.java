package de.ct.manager.schedule.plan;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.plan.PersonalScheduleService.PersonalService;
import de.ct.manager.schedule.user.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/plan/mine")
public class PersonalScheduleController {

    private static final MediaType CALENDAR = new MediaType("text", "calendar", java.nio.charset.StandardCharsets.UTF_8);

    private final PersonalScheduleService personalScheduleService;

    public PersonalScheduleController(PersonalScheduleService personalScheduleService) {
        this.personalScheduleService = personalScheduleService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonalService>> getServices(@RequestParam(defaultValue = "false") boolean past,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(personalScheduleService.getServices(currentUser.getUsername(), past));
    }

    @GetMapping("/ics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadIcs(@RequestParam(defaultValue = "false") boolean past,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<PersonalService> services = personalScheduleService.getServices(currentUser.getUsername(), past);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dienste.ics")
            .contentType(CALENDAR)
            .body(personalScheduleService.toIcs(services));
    }
}
