package de.ct.manager.schedule.user;

/**
 * Rights an administrator can grant to or revoke from a user at runtime, unlike roles which are
 * only assigned in the database. WebSecurityConfig#roleHierarchy defines what each right unlocks.
 */
public enum UserPermission {
    PERMISSION_SCHEDULE_MANAGE, PERMISSION_ALARM_MANAGE, PERMISSION_MAINTENANCE_MANAGE,
    /** "Betriebsadmin": manages the start/end of operations checklists, approves completed ones and reads the archive. */
    PERMISSION_OPERATIONS_MANAGE
}
