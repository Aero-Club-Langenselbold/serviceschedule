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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectCommentRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectDetail;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectStatusRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

/** The defect book: every signed in user reports and works on defects; finer rules are decided in the service. */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance")
@PreAuthorize("isAuthenticated()")
public class MaintenanceDefectController {

    private final MaintenanceDefectService defectService;

    public MaintenanceDefectController(MaintenanceDefectService defectService) {
        this.defectService = defectService;
    }

    /** The defect book of all aircraft and equipment, or of one with aircraftId. */
    @GetMapping("/defects")
    public ResponseEntity<List<DefectResponse>> getAllDefects(@RequestParam(required = false) String aircraftId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.getDefects(aircraftId, currentUser));
    }

    @GetMapping("/aircraft/{aircraftId}/defects")
    public ResponseEntity<List<DefectResponse>> getDefects(@PathVariable String aircraftId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.getDefects(aircraftId, currentUser));
    }

    @GetMapping("/defects/assignable-users")
    public ResponseEntity<List<Person>> getAssignableUsers(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.getAssignableUsers(currentUser));
    }

    @GetMapping("/defects/{id}")
    public ResponseEntity<DefectDetail> getDefect(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.getDefect(id, currentUser));
    }

    @PostMapping("/aircraft/{aircraftId}/defects")
    public ResponseEntity<DefectDetail> create(@PathVariable String aircraftId, @Valid @RequestBody DefectRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(defectService.create(aircraftId, request, currentUser));
    }

    @PutMapping("/defects/{id}")
    public ResponseEntity<DefectDetail> update(@PathVariable String id, @Valid @RequestBody DefectRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.update(id, request, currentUser));
    }

    @PutMapping("/defects/{id}/status")
    public ResponseEntity<DefectDetail> setStatus(@PathVariable String id, @Valid @RequestBody DefectStatusRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.setStatus(id, request.status(), request.comment(), currentUser));
    }

    @PostMapping("/defects/{id}/comments")
    public ResponseEntity<DefectDetail> addComment(@PathVariable String id, @Valid @RequestBody DefectCommentRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(defectService.addComment(id, request.content(), currentUser));
    }

    @DeleteMapping("/defects/{id}/comments/{commentId}")
    public ResponseEntity<DefectDetail> deleteComment(@PathVariable String id, @PathVariable String commentId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.deleteComment(id, commentId, currentUser));
    }

    @PostMapping("/defects/{id}/sign")
    public ResponseEntity<DefectDetail> signOff(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.signOff(id, currentUser));
    }

    @DeleteMapping("/defects/{id}/sign")
    public ResponseEntity<DefectDetail> withdrawSignOff(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(defectService.withdrawSignOff(id, currentUser));
    }

    @DeleteMapping("/defects/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        defectService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
