package de.ct.manager.schedule.notification;

public enum NotificationType {
    /** A defect of the defect book was assigned to the user. */
    DEFECT_ASSIGNED,
    /** An operations checklist was confirmed complete and waits for the approval of an operations administrator. */
    CHECKLIST_SUBMITTED
}
