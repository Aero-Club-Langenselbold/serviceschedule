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

import de.ct.manager.schedule.maintenance.MaintenanceDtos.TemplateRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TemplateResponse;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

/** Templates are an administration tool, only maintenance administrators see them. */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance/templates")
@PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
public class MaintenanceTemplateController {

    private final MaintenanceTemplateService templateService;

    public MaintenanceTemplateController(MaintenanceTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getTemplates() {
        return ResponseEntity.ok(templateService.getTemplates());
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(@PathVariable String id, @Valid @RequestBody TemplateRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(templateService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        templateService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
