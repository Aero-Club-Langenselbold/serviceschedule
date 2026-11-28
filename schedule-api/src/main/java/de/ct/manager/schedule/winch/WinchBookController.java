package de.ct.manager.schedule.winch;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.winch.WinchDtos.DayDetail;
import de.ct.manager.schedule.winch.WinchDtos.DaySummary;
import de.ct.manager.schedule.winch.WinchDtos.IncidentRequest;
import de.ct.manager.schedule.winch.WinchDtos.OptionCreateRequest;
import de.ct.manager.schedule.winch.WinchDtos.OptionUpdateRequest;
import de.ct.manager.schedule.winch.WinchDtos.OrderRequest;
import de.ct.manager.schedule.winch.WinchDtos.RemarkRequest;
import de.ct.manager.schedule.winch.WinchDtos.SettingsResponse;
import jakarta.validation.Valid;

/**
 * The winch book. It lives under /maintenance so kiosk sessions reach it; who may do what is decided in
 * {@link WinchBookService}.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance/winch")
@PreAuthorize("isAuthenticated()")
public class WinchBookController {

    private final WinchBookService winchBookService;

    public WinchBookController(WinchBookService winchBookService) {
        this.winchBookService = winchBookService;
    }

    // --- settings: ropes and incident types

    @GetMapping("/settings")
    public ResponseEntity<SettingsResponse> getSettings() {
        return ResponseEntity.ok(winchBookService.getSettings());
    }

    @PostMapping("/settings/{kind}")
    public ResponseEntity<SettingsResponse> createOption(@PathVariable String kind, @Valid @RequestBody OptionCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(winchBookService.createOption(kind(kind), request.name(), currentUser));
    }

    @PutMapping("/settings/{kind}/order")
    public ResponseEntity<SettingsResponse> reorder(@PathVariable String kind, @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.reorder(kind(kind), request.ids(), currentUser));
    }

    @PutMapping("/settings/{kind}/{id}")
    public ResponseEntity<SettingsResponse> updateOption(@PathVariable String kind, @PathVariable String id, @Valid @RequestBody OptionUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.updateOption(kind(kind), id, request.name(), request.active(), currentUser));
    }

    /** Deletes the option, or deactivates it if entries refer to it. */
    @DeleteMapping("/settings/{kind}/{id}")
    public ResponseEntity<SettingsResponse> removeOption(@PathVariable String kind, @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.removeOption(kind(kind), id, currentUser));
    }

    private static WinchOptionKind kind(String kind) {
        return switch (kind) {
            case "ropes" -> WinchOptionKind.ROPES;
            case "incident-types" -> WinchOptionKind.INCIDENT_TYPES;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown list " + kind);
        };
    }

    // --- days

    /** The pages of a year (default: the current one), newest first. */
    @GetMapping("/days")
    public ResponseEntity<Page<DaySummary>> listDays(@RequestParam(required = false) Integer year,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(winchBookService.listDays(year == null ? winchBookService.today().getYear() : year, pageable));
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> years() {
        return ResponseEntity.ok(winchBookService.years());
    }

    @GetMapping("/days/today")
    public ResponseEntity<DayDetail> getToday(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.getDay(winchBookService.today(), currentUser));
    }

    @GetMapping("/days/{date}")
    public ResponseEntity<DayDetail> getDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.getDay(date, currentUser));
    }

    @PutMapping("/days/{date}/remark")
    public ResponseEntity<DayDetail> saveDayRemark(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody RemarkRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.saveDayRemark(date, request, currentUser));
    }

    @PutMapping("/days/{date}/ropes/{ropeId}/remark")
    public ResponseEntity<DayDetail> saveRopeRemark(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String ropeId, @Valid @RequestBody RemarkRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.saveRopeRemark(date, ropeId, request, currentUser));
    }

    // --- incidents and photos, always recorded for today

    @PostMapping("/incidents")
    public ResponseEntity<DayDetail> addIncident(@Valid @RequestBody IncidentRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(winchBookService.addIncident(request, currentUser));
    }

    @DeleteMapping("/incidents/{id}")
    public ResponseEntity<DayDetail> deleteIncident(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.deleteIncident(id, currentUser));
    }

    @PostMapping(path = "/ropes/{ropeId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DayDetail> addPhoto(@PathVariable String ropeId, @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(winchBookService.addPhoto(ropeId, file, currentUser));
    }

    @GetMapping("/photos/{id}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String id) {
        WinchBookService.PhotoData photo = winchBookService.getPhoto(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(photo.contentType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(photo.filename() == null ? id : photo.filename(), StandardCharsets.UTF_8).build());
        // photos never change, they are only deleted
        headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate());
        return new ResponseEntity<>(photo.data(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/photos/{id}")
    public ResponseEntity<DayDetail> deletePhoto(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(winchBookService.deletePhoto(id, currentUser));
    }
}
