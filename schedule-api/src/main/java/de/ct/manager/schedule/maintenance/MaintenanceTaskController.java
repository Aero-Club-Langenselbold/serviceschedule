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

import de.ct.manager.schedule.maintenance.MaintenanceDtos.ChecklistToggleRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.NoteRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskDetail;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskStatusRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskSummary;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance")
public class MaintenanceTaskController {

    private final MaintenanceTaskService taskService;

    public MaintenanceTaskController(MaintenanceTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/aircraft/{aircraftId}/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskSummary>> getTasks(@PathVariable String aircraftId) {
        return ResponseEntity.ok(taskService.getTasks(aircraftId));
    }

    @PostMapping("/aircraft/{aircraftId}/tasks")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<TaskDetail> create(@PathVariable String aircraftId, @Valid @RequestBody TaskRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(aircraftId, request, currentUser));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> getTask(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.getTask(id, currentUser));
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<TaskDetail> update(@PathVariable String id, @Valid @RequestBody TaskRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.update(id, request, currentUser));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        taskService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks/{id}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> start(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.start(id, currentUser));
    }

    @PostMapping("/tasks/{id}/reset")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> reset(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.reset(id, currentUser));
    }

    @PostMapping("/tasks/{id}/finish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> finish(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.finish(id, currentUser));
    }

    @PostMapping("/tasks/{id}/reopen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> reopen(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.reopen(id, currentUser));
    }

    @PutMapping("/tasks/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> moveTo(@PathVariable String id, @Valid @RequestBody TaskStatusRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.moveTo(id, request.status(), currentUser));
    }

    @PostMapping("/tasks/{id}/sign")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<TaskDetail> sign(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.sign(id, currentUser));
    }

    @DeleteMapping("/tasks/{id}/sign")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<TaskDetail> unsign(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.unsign(id, currentUser));
    }

    @PutMapping("/tasks/{id}/checklist/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> toggleChecklistItem(@PathVariable String id, @PathVariable String itemId, @RequestBody ChecklistToggleRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.toggleChecklistItem(id, itemId, request.done(), currentUser));
    }

    @PostMapping("/tasks/{id}/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> addNote(@PathVariable String id, @Valid @RequestBody NoteRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.addNote(id, request.content(), currentUser));
    }

    @DeleteMapping("/tasks/{id}/notes/{noteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetail> deleteNote(@PathVariable String id, @PathVariable String noteId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(taskService.deleteNote(id, noteId, currentUser));
    }
}
