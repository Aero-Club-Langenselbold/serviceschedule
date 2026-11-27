package de.ct.manager.schedule.user;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.authentication.JwtResponse;
import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.security.JwtUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ParticipantRepository participantRepository;
    private final JwtUtils jwtUtils;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, ParticipantRepository participantRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.participantRepository = participantRepository;
        this.jwtUtils = jwtUtils;
    }

    public Page<UserOverview> getUsers(Pageable pageable, String search) {
        Page<User> users = search == null || search.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findAll(pageable, search);

        List<String> usernames = users.map(User::getUsername).getContent();
        Map<String, Participant> participants = participantRepository.findAllByEmailIn(usernames).stream()
                .collect(Collectors.toMap(Participant::getEmail, Function.identity(), (first, second) -> first));

        return users.map(user -> toOverview(user, participants.get(user.getUsername())));
    }

    @Transactional
    public UserOverview updatePermissions(String id, Set<UserPermission> permissions, String changedBy) {
        User user = findUser(id);
        user.setPermissions(new HashSet<>(permissions));
        User saved = userRepository.save(user);
        log.info("User {} set permissions of {} to {}", changedBy, saved.getUsername(), permissions);

        return toOverview(saved, participantRepository.findByEmail(saved.getUsername()).orElse(null));
    }

    /**
     * Sets the assignable roles of a user (see {@link UserRole#assignable()}); ROLE_USER stays as it is, and a user left
     * without any role gets ROLE_USER. Administrators cannot take away their own administrator role, so there is always one left.
     */
    @Transactional
    public UserOverview updateRoles(String id, Set<UserRole> roles, String changedBy) {
        User user = findUser(id);
        if (user.getUsername().equals(changedBy) && isAdmin(user) && !roles.contains(UserRole.ROLE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot revoke your own administrator role");
        }
        Set<UserRole> wanted = roles.stream().filter(UserRole.assignable()::contains).collect(Collectors.toCollection(HashSet::new));
        if (user.getRoles().stream().anyMatch(role -> role.getName() == UserRole.ROLE_USER) || wanted.isEmpty()) {
            wanted.add(UserRole.ROLE_USER);
        }
        user.setRoles(wanted.stream().map(this::findOrCreateRole).collect(Collectors.toCollection(HashSet::new)));
        User saved = userRepository.save(user);
        log.info("User {} set roles of {} to {}", changedBy, saved.getUsername(), wanted);

        return toOverview(saved, participantRepository.findByEmail(saved.getUsername()).orElse(null));
    }

    /** Roles are reference data seeded by migrations; a missing one is created rather than failing. */
    private Role findOrCreateRole(UserRole name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    public JwtResponse impersonate(String id, String impersonator) {
        User target = findUser(id);
        if (target.getUsername().equals(impersonator)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot impersonate yourself");
        }
        if (isAdmin(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot impersonate an administrator");
        }
        log.info("User {} started impersonating {}", impersonator, target.getUsername());

        UserDetailsImpl details = UserDetailsImpl.build(target);
        return JwtResponse.of(jwtUtils.generateImpersonationToken(details, impersonator), details, impersonator);
    }

    public JwtResponse stopImpersonation(String token) {
        String impersonator = token == null ? null : jwtUtils.getImpersonatorFromJwtToken(token);
        if (impersonator == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not impersonating anyone");
        }

        // The administrator gets a fresh token, so they must still exist and still be an administrator.
        User admin = userRepository.findByUsername(impersonator)
                .filter(this::isAdmin)
                .orElseThrow(() -> new AccessDeniedException("Impersonator is no longer an administrator"));
        log.info("User {} stopped impersonating", impersonator);

        UserDetailsImpl details = UserDetailsImpl.build(admin);
        return JwtResponse.of(jwtUtils.generateJwtToken(details), details, null);
    }

    private User findUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == UserRole.ROLE_ADMIN);
    }

    private UserOverview toOverview(User user, Participant participant) {
        return new UserOverview(
                user.getId(),
                user.getUsername(),
                participant != null ? participant.getFullname() : null,
                user.getRoles().stream().map(role -> role.getName().name()).sorted().toList(),
                user.getPermissions().stream().map(UserPermission::name).sorted().toList());
    }
}
