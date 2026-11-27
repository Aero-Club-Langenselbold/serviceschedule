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

import de.ct.manager.schedule.maintenance.MaintenanceDtos.InventoryHistoryResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.InventoryItemRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.InventoryItemResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.InventoryQuantityRequest;
import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/maintenance/inventory")
public class MaintenanceInventoryController {

    private final MaintenanceInventoryService inventoryService;

    public MaintenanceInventoryController(MaintenanceInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InventoryItemResponse>> getItems() {
        return ResponseEntity.ok(inventoryService.getItems());
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InventoryHistoryResponse>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(inventoryService.getHistory(id));
    }

    /** Every signed in user counts the stock. */
    @PutMapping("/{id}/quantity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventoryItemResponse> count(@PathVariable String id, @Valid @RequestBody InventoryQuantityRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(inventoryService.count(id, request.quantity(), currentUser));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
    public ResponseEntity<InventoryItemResponse> update(@PathVariable String id, @Valid @RequestBody InventoryItemRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(inventoryService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MAINTENANCE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        inventoryService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
