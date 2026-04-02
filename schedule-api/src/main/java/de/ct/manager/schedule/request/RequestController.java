package de.ct.manager.schedule.request;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.user.UserDetailsImpl;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/request")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<Request>> listRequests() {
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(requestService.getAllRequests());
        } else {
            String currentUser = ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
            return ResponseEntity.ok(requestService.getUserRequests(currentUser));
        }
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('USER', 'SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<Request>> getRequestsByPage(@SortDefault(sort = "createdAt", direction = Direction.DESC) @PageableDefault(size = 20) final Pageable pageable) {
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(requestService.getRequestsByPage(pageable));
        } else {
            String currentUser = ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
            return ResponseEntity.ok(requestService.getUserRequestsByPage(currentUser, pageable));
        }
    }

    @GetMapping("/fetch/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<Request> getRequest(@PathVariable String id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteRequest(@PathVariable String id) {
        requestService.deleteRequest(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('USER', 'SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<Request> createOrUpdateRequest(@RequestBody Request request) {
        return ResponseEntity.ok(requestService.createOrUpdateRequest(request));
    }
}
