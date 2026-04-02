package de.ct.manager.schedule.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import de.ct.manager.schedule.user.UserRole;

public class TestUtils {
    public static final String TEST_ADMIN = "admin@test.de";
    public static final String TEST_ADMIN_PW = "won't_tell";
    public static final String TEST_USER = "test@test.com";
    public static final String TEST_USER_PW = "won't_tell_either";

    public static Collection<GrantedAuthority> createAuthorities(UserRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return role.name();
            }
        });

        return authorities;
    }

    public static UserDto getUserForRole(UserRole role) {
        if (role == UserRole.ROLE_ADMIN || role == UserRole.ROLE_ALARM_ADMIN) {
            return new UserDto(TEST_ADMIN, TEST_ADMIN_PW);
        } else {
            return new UserDto(TEST_USER, TEST_USER_PW);
        }
    }
}
