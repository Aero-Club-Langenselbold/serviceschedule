package de.ct.manager.schedule.notification;

import java.time.Instant;
import java.util.List;

import de.ct.manager.schedule.maintenance.DefectStatus;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record DefectSummary(String id, String title, String aircraftId, String aircraftLabel, DefectStatus status) {
    }

    public record NotificationResponse(String id, NotificationType type, Instant createdAt, boolean read, Person actor, DefectSummary defect) {
    }

    /** The latest notifications of the user and how many of all their notifications are unread. */
    public record NotificationsResponse(long unreadCount, List<NotificationResponse> items) {
    }
}
