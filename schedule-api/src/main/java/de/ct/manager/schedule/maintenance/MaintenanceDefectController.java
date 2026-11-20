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

import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectStatusRequest;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

/** Every signed in user reports defects; who may edit or delete one is decided in the service. */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance")
@PreAuthorize("isAuthenticated()")
public class MaintenanceDefectController {

    private final MaintenanceDefectService defectService;

    public MaintenanceDefectController(MaintenanceDefectService defectService) {
        this.defectService = defectService;
    }

    @GetMapping("/aircraft/{aircraftId}/defects")
    public ResponseEntity<List<DefectResponse>> getDefects(@PathVariable String aircraftId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.getDefects(aircraftId, currentUser));
    }

    @PostMapping("/aircraft/{aircraftId}/defects")
    public ResponseEntity<DefectResponse> create(@PathVariable String aircraftId, @Valid @RequestBody DefectRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(defectService.create(aircraftId, request, currentUser));
    }

    @PutMapping("/defects/{id}")
    public ResponseEntity<DefectResponse> update(@PathVariable String id, @Valid @RequestBody DefectRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.update(id, request, currentUser));
    }

    @PutMapping("/defects/{id}/status")
    public ResponseEntity<DefectResponse> setStatus(@PathVariable String id, @Valid @RequestBody DefectStatusRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.setStatus(id, request.status(), currentUser));
    }

    @DeleteMapping("/defects/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        defectService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
