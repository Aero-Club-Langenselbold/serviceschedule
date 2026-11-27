package de.ct.manager.schedule.operations;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRequest;
import de.ct.manager.schedule.operations.OperationsDtos.ChecklistDetail;
import de.ct.manager.schedule.operations.OperationsDtos.ChecklistSummary;
import de.ct.manager.schedule.operations.OperationsDtos.ItemRequest;
import de.ct.manager.schedule.operations.OperationsDtos.OverviewResponse;
import de.ct.manager.schedule.operations.OperationsDtos.TemplateRequest;
import de.ct.manager.schedule.operations.OperationsDtos.TemplateResponse;
import de.ct.manager.schedule.operations.OperationsDtos.TodayResponse;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

/** Start and end of operations checklists; who may do what is decided in {@link OperationsChecklistService}. */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/operations")
@PreAuthorize("isAuthenticated()")
public class OperationsChecklistController {

    private final OperationsChecklistService checklistService;

    public OperationsChecklistController(OperationsChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> getOverview(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.getOverview(currentUser));
    }

    @GetMapping("/aircraft/{aircraftId}/templates/{kind}")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable String aircraftId, @PathVariable ChecklistKind kind) {
        return ResponseEntity.ok(checklistService.getTemplate(aircraftId, kind));
    }

    @PutMapping("/aircraft/{aircraftId}/templates/{kind}")
    public ResponseEntity<TemplateResponse> saveTemplate(@PathVariable String aircraftId, @PathVariable ChecklistKind kind,
            @Valid @RequestBody TemplateRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.saveTemplate(aircraftId, kind, request, currentUser));
    }

    @DeleteMapping("/aircraft/{aircraftId}/templates/{kind}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String aircraftId, @PathVariable ChecklistKind kind,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        checklistService.deleteTemplate(aircraftId, kind, currentUser);
        return ResponseEntity.noContent().build();
    }

    /** Target of the QR code: the checklist of today, or what is needed to start it. */
    @GetMapping("/aircraft/{aircraftId}/checklists/{kind}/today")
    public ResponseEntity<TodayResponse> getToday(@PathVariable String aircraftId, @PathVariable ChecklistKind kind,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.getToday(aircraftId, kind, currentUser));
    }

    @PostMapping("/aircraft/{aircraftId}/checklists/{kind}/today")
    public ResponseEntity<ChecklistDetail> start(@PathVariable String aircraftId, @PathVariable ChecklistKind kind,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.start(aircraftId, kind, currentUser));
    }

    /** The archive, for operations administrators. */
    @GetMapping("/checklists")
    public ResponseEntity<Page<ChecklistSummary>> search(@RequestParam(required = false) String aircraftId,
            @RequestParam(required = false) ChecklistKind kind, @RequestParam(required = false) ChecklistStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = { "date", "createdAt" }, direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.search(aircraftId, kind, status, from, to, search, pageable, currentUser));
    }

    @GetMapping("/checklists/{id}")
    public ResponseEntity<ChecklistDetail> getChecklist(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.getChecklist(id, currentUser));
    }

    @PutMapping("/checklists/{id}/items/{itemId}")
    public ResponseEntity<ChecklistDetail> checkItem(@PathVariable String id, @PathVariable String itemId, @Valid @RequestBody ItemRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.checkItem(id, itemId, request, currentUser));
    }

    @PostMapping("/checklists/{id}/items/{itemId}/defect")
    public ResponseEntity<ChecklistDetail> reportDefect(@PathVariable String id, @PathVariable String itemId, @Valid @RequestBody DefectRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.reportDefect(id, itemId, request, currentUser));
    }

    @PostMapping("/checklists/{id}/submit")
    public ResponseEntity<ChecklistDetail> submit(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.submit(id, currentUser));
    }

    @PostMapping("/checklists/{id}/approve")
    public ResponseEntity<ChecklistDetail> approve(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.approve(id, currentUser));
    }

    @PostMapping("/checklists/{id}/reopen")
    public ResponseEntity<ChecklistDetail> reopen(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(checklistService.reopen(id, currentUser));
    }
}
