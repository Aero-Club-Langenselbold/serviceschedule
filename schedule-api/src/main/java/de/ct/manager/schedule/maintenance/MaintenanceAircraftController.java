package de.ct.manager.schedule.maintenance;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.AircraftRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.AircraftResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.ImportResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TemplateImportRequest;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance/aircraft")
public class MaintenanceAircraftController {

    private final MaintenanceAircraftService aircraftService;

    public MaintenanceAircraftController(MaintenanceAircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AircraftResponse>> getAircraft() {
        return ResponseEntity.ok(aircraftService.getAircraft());
    }

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getTypes() {
        return ResponseEntity.ok(aircraftService.getTypes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AircraftResponse> getAircraft(@PathVariable String id) {
        return ResponseEntity.ok(aircraftService.getAircraft(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
    public ResponseEntity<AircraftResponse> create(@Valid @RequestBody AircraftRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aircraftService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
    public ResponseEntity<AircraftResponse> update(@PathVariable String id, @Valid @RequestBody AircraftRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(aircraftService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        aircraftService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/import-templates")
    @PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
    public ResponseEntity<ImportResponse> importTemplates(@PathVariable String id, @Valid @RequestBody TemplateImportRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(aircraftService.importTemplates(id, request, currentUser));
    }
}
