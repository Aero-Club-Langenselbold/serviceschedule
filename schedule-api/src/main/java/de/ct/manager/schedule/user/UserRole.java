package de.ct.manager.schedule.user;

import java.util.Arrays;
import java.util.List;

public enum UserRole {
    ROLE_USER, ROLE_ADMIN, ROLE_ALARM_ADMIN, ROLE_SCHEDULE_ADMIN, ROLE_MAINTENANCE_ADMIN;

    /** Roles administrators assign on the users page; ROLE_USER is the base role every member gets at registration. */
    public static List<UserRole> assignable() {
        return Arrays.stream(values()).filter(role -> role != ROLE_USER).toList();
    }
}
