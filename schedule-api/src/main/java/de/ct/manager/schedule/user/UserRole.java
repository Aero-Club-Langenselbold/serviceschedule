package de.ct.manager.schedule.user;

import java.util.Arrays;
import java.util.List;

public enum UserRole {
    ROLE_USER, ROLE_ADMIN;

    /**
     * Roles administrators assign on the users page; ROLE_USER is the base role every member gets at registration.
     * There are no further roles: what a member may administer is granted with {@link UserPermission}s.
     */
    public static List<UserRole> assignable() {
        return Arrays.stream(values()).filter(role -> role != ROLE_USER).toList();
    }
}
