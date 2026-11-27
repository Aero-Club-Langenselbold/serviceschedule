package de.ct.manager.schedule.user;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    /** Authority of sessions opened through the kiosk mode. */
    public static final String KIOSK = "KIOSK";

    private String id;
    private String username;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authoritiesOf(user));
    }

    /**
     * The same user signed in through the kiosk mode (name + PIN on a shared tablet): a plain member, rights and the
     * administrator role are ignored. {@link #KIOSK} marks the session, the security configuration limits it to the
     * maintenance and operations API.
     */
    public static UserDetailsImpl kiosk(UserDetailsImpl user) {
        return kiosk(user.getId(), user.getUsername(), user.getPassword());
    }

    public static UserDetailsImpl kiosk(User user) {
        return kiosk(user.getId(), user.getUsername(), user.getPassword());
    }

    private static UserDetailsImpl kiosk(String id, String username, String password) {
        return new UserDetailsImpl(id, username, password,
                List.of(new SimpleGrantedAuthority(UserRole.ROLE_USER.name()), new SimpleGrantedAuthority(KIOSK)));
    }

    public boolean isKiosk() {
        return authorities.stream().anyMatch(authority -> KIOSK.equals(authority.getAuthority()));
    }

    public static List<GrantedAuthority> authoritiesOf(User user) {
        return Stream.concat(
                user.getRoles().stream().map(role -> role.getName().name()),
                user.getPermissions().stream().map(UserPermission::name))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
