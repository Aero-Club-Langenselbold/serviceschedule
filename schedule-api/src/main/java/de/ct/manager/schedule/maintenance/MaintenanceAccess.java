package de.ct.manager.schedule.maintenance;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;
import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRepository;
import de.ct.manager.schedule.user.UserPermission;

/** Who is calling, what they may do in the maintenance module and how people are named in responses. */
@Component
public class MaintenanceAccess {

    private final RoleHierarchy roleHierarchy;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public MaintenanceAccess(RoleHierarchy roleHierarchy, UserRepository userRepository, ParticipantRepository participantRepository) {
        this.roleHierarchy = roleHierarchy;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    /** Administrators and users holding the maintenance right. */
    public boolean canManage(UserDetailsImpl currentUser) {
        return holds(currentUser, UserPermission.PERMISSION_MAINTENANCE_MANAGE);
    }

    /** Administrators and users holding the operations right ("Betriebsadmin"). */
    public boolean canManageOperations(UserDetailsImpl currentUser) {
        return holds(currentUser, UserPermission.PERMISSION_OPERATIONS_MANAGE);
    }

    /** Whether the user holds the right, directly or through the role hierarchy (administrators hold all rights). */
    public boolean holds(UserDetailsImpl currentUser, UserPermission permission) {
        return currentUser != null && roleHierarchy.getReachableGrantedAuthorities(currentUser.getAuthorities()).stream()
                .anyMatch(authority -> permission.name().equals(authority.getAuthority()));
    }

    public void requireManage(UserDetailsImpl currentUser) {
        if (!canManage(currentUser)) {
            throw new AccessDeniedException("Maintenance administrators only");
        }
    }

    public User currentUser(UserDetailsImpl currentUser) {
        return userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }

    public static boolean isSameUser(User user, UserDetailsImpl currentUser) {
        return user != null && currentUser != null && Objects.equals(user.getUsername(), currentUser.getUsername());
    }

    /** Resolves the display names of the given users with a single query. */
    public Names names(Collection<User> users) {
        var usernames = users.stream().filter(Objects::nonNull).map(User::getUsername).distinct().toList();
        Map<String, String> fullnames = usernames.isEmpty() ? Map.of() : participantRepository.findAllByEmailIn(usernames).stream()
                .collect(Collectors.toMap(Participant::getEmail, Participant::getFullname, (first, second) -> first));
        return new Names(fullnames);
    }

    public static final class Names {
        private final Map<String, String> fullnames;

        private Names(Map<String, String> fullnames) {
            this.fullnames = fullnames;
        }

        public Person of(User user) {
            return user == null ? null : new Person(user.getId(), fullnames.getOrDefault(user.getUsername(), user.getUsername()));
        }
    }

    public static ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found");
    }
}
