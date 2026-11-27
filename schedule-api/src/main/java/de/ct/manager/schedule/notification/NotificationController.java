package de.ct.manager.schedule.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.notification.NotificationDtos.NotificationsResponse;
import de.ct.manager.schedule.user.UserDetailsImpl;

/** The notifications of the signed in user; nobody sees or changes those of others. */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationsResponse> getNotifications(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(notificationService.getNotifications(currentUser));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        notificationService.markRead(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        notificationService.markAllRead(currentUser);
        return ResponseEntity.noContent().build();
    }
}
