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

import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotResponse;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance/workshop")
public class MaintenanceWorkshopController {

    private final MaintenanceWorkshopService workshopService;

    public MaintenanceWorkshopController(MaintenanceWorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkshopSlotResponse>> getSlots() {
        return ResponseEntity.ok(workshopService.getSlots());
    }

    @PostMapping
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<WorkshopSlotResponse> create(@Valid @RequestBody WorkshopSlotRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workshopService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<WorkshopSlotResponse> update(@PathVariable String id, @Valid @RequestBody WorkshopSlotRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(workshopService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        workshopService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
