package de.ct.manager.schedule.notification;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ct.manager.schedule.maintenance.MaintenanceAccess;
import de.ct.manager.schedule.maintenance.MaintenanceAccess.Names;
import de.ct.manager.schedule.maintenance.MaintenanceDefect;
import de.ct.manager.schedule.notification.NotificationDtos.ChecklistRef;
import de.ct.manager.schedule.notification.NotificationDtos.DefectSummary;
import de.ct.manager.schedule.notification.NotificationDtos.NotificationResponse;
import de.ct.manager.schedule.notification.NotificationDtos.NotificationsResponse;
import de.ct.manager.schedule.operations.OperationsChecklist;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserPermission;
import de.ct.manager.schedule.user.UserRepository;

/** Notifications in the header: created by other modules, read and dismissed by their recipient only. */
@Service
public class NotificationService {

    /** The header shows the latest notifications only. */
    static final int LIMIT = 30;

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MaintenanceAccess access;

    public NotificationService(UserNotificationRepository notificationRepository, UserRepository userRepository, MaintenanceAccess access) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    /** Tells the user that the defect was assigned to them; nobody is notified about their own action. */
    @Transactional
    public void defectAssigned(MaintenanceDefect defect, User assignee, User actor) {
        if (actor != null && Objects.equals(actor.getId(), assignee.getId())) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setUser(assignee);
        notification.setType(NotificationType.DEFECT_ASSIGNED);
        notification.setDefect(defect);
        notification.setActor(actor);
        notificationRepository.save(notification);
    }

    /** An assignment that was taken back before it was read is not news anymore. */
    @Transactional
    public void defectUnassigned(MaintenanceDefect defect, User assignee) {
        notificationRepository.deleteUnreadByDefectAndUser(defect, assignee);
    }

    /**
     * Tells the operations administrators that a checklist waits for their approval. Only users granted the right are
     * notified, not every administrator; nobody is notified about their own action.
     */
    @Transactional
    public void checklistSubmitted(OperationsChecklist checklist, User actor) {
        userRepository.findAllWithPermission(UserPermission.PERMISSION_OPERATIONS_MANAGE).stream()
                .filter(user -> actor == null || !Objects.equals(actor.getId(), user.getId()))
                .forEach(user -> {
                    UserNotification notification = new UserNotification();
                    notification.setUser(user);
                    notification.setType(NotificationType.CHECKLIST_SUBMITTED);
                    notification.setChecklist(checklist);
                    notification.setActor(actor);
                    notificationRepository.save(notification);
                });
    }

    /** Once one operations administrator approved the checklist, it is no news for the others anymore. */
    @Transactional
    public void checklistDone(OperationsChecklist checklist) {
        notificationRepository.markReadByChecklist(checklist, Instant.now());
    }

    /** A checklist sent back to be filled in again does not wait for approval anymore. */
    @Transactional
    public void checklistReopened(OperationsChecklist checklist) {
        notificationRepository.deleteUnreadByChecklist(checklist);
    }

    @Transactional(readOnly = true)
    public NotificationsResponse getNotifications(UserDetailsImpl currentUser) {
        User user = access.currentUser(currentUser);
        List<UserNotification> notifications = notificationRepository.findAllByUserOrderByCreatedAtDescIdDesc(user, PageRequest.of(0, LIMIT));
        Names names = access.names(notifications.stream().map(UserNotification::getActor).toList());
        return new NotificationsResponse(notificationRepository.countByUserAndReadAtIsNull(user),
                notifications.stream().map(notification -> toResponse(notification, names)).toList());
    }

    @Transactional
    public void markRead(String id, UserDetailsImpl currentUser) {
        UserNotification notification = notificationRepository.findByIdAndUser(id, access.currentUser(currentUser))
                .orElseThrow(() -> MaintenanceAccess.notFound("Notification"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public void markAllRead(UserDetailsImpl currentUser) {
        Instant now = Instant.now();
        notificationRepository.findAllByUserAndReadAtIsNull(access.currentUser(currentUser)).forEach(notification -> notification.setReadAt(now));
    }

    private static NotificationResponse toResponse(UserNotification notification, Names names) {
        MaintenanceDefect defect = notification.getDefect();
        DefectSummary summary = defect == null ? null : new DefectSummary(defect.getId(), defect.getTitle(), defect.getAircraft().getId(),
                defect.getAircraft().getRegistration(), defect.getStatus());
        OperationsChecklist checklist = notification.getChecklist();
        ChecklistRef checklistRef = checklist == null ? null : new ChecklistRef(checklist.getId(), checklist.getAircraft().getId(),
                checklist.getAircraft().getRegistration(), checklist.getKind(), checklist.getDate(), checklist.getStatus());
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getCreatedAt(), notification.getReadAt() != null,
                names.of(notification.getActor()), summary, checklistRef);
    }
}
