package de.ct.manager.schedule.user;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.authentication.JwtResponse;
import de.ct.manager.schedule.security.AuthTokenFilter;
import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserOverview>> getUsers(@SortDefault(sort = "username") @PageableDefault(size = 20) final Pageable pageable, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.getUsers(pageable, search));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserPermission>> getPermissions() {
        return ResponseEntity.ok(List.of(UserPermission.values()));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserOverview> updatePermissions(@PathVariable String id, @RequestBody Set<UserPermission> permissions, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(userService.updatePermissions(id, permissions, currentUser.getUsername()));
    }

    @PostMapping("/{id}/impersonate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JwtResponse> impersonate(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(userService.impersonate(id, currentUser.getUsername()));
    }

    // Open to any authenticated user: during an impersonation the caller holds the rights of the impersonated user.
    @PostMapping("/impersonation/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JwtResponse> stopImpersonation(HttpServletRequest request) {
        return ResponseEntity.ok(userService.stopImpersonation(AuthTokenFilter.parseJwt(request)));
    }
}
