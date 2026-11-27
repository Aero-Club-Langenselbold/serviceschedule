package de.ct.manager.schedule.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import de.ct.manager.schedule.maintenance.DefectStatus;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;
import de.ct.manager.schedule.operations.ChecklistKind;
import de.ct.manager.schedule.operations.ChecklistStatus;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record DefectSummary(String id, String title, String aircraftId, String aircraftLabel, DefectStatus status) {
    }

    public record ChecklistRef(String id, String aircraftId, String registration, ChecklistKind kind, LocalDate date, ChecklistStatus status) {
    }

    /** defect for DEFECT_ASSIGNED, checklist for CHECKLIST_SUBMITTED. */
    public record NotificationResponse(String id, NotificationType type, Instant createdAt, boolean read, Person actor, DefectSummary defect,
            ChecklistRef checklist) {
    }

    /** The latest notifications of the user and how many of all their notifications are unread. */
    public record NotificationsResponse(long unreadCount, List<NotificationResponse> items) {
    }
}
