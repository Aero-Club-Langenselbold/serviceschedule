package de.ct.manager.schedule.authentication;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRepository;
import de.ct.manager.schedule.user.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * Sign in through the kiosk mode with e-mail address and PIN. Works the same with and without the Vereinsflieger login,
 * the PIN is checked locally (users set it after a regular sign in). Users without PIN cannot sign in this way.
 * A PIN has only a million combinations, so a user is locked out of the kiosk sign in for a while after a few
 * failed attempts.
 */
@Service
@Slf4j
public class KioskAuthService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant lockedUntil) {
    }

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    public KioskAuthService(UserRepository userRepository, UserService userService, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    public JwtResponse signIn(KioskLoginRequest request) {
        String username = request.username().trim();
        String key = username.toLowerCase(Locale.ROOT);
        Attempts previous = attempts.get(key);
        if (previous != null && previous.lockedUntil() != null && previous.lockedUntil().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts");
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !userService.matchesKioskPin(user, request.pin())) {
            recordFailure(key);
            // same answer for unknown users, users without PIN and wrong PINs
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or PIN");
        }
        attempts.remove(key);

        UserDetailsImpl details = UserDetailsImpl.kiosk(user);
        log.info("User {} signed in through the kiosk mode", user.getUsername());
        return JwtResponse.of(jwtUtils.generateKioskToken(details), details, null);
    }

    private void recordFailure(String key) {
        attempts.compute(key, (k, current) -> {
            Instant now = clock.instant();
            int failures = current == null || (current.lockedUntil() != null && !current.lockedUntil().isAfter(now))
                    ? 1 : current.failures() + 1;
            if (failures >= MAX_FAILED_ATTEMPTS) {
                log.warn("Kiosk sign in for {} locked after {} failed attempts", k, failures);
                return new Attempts(0, now.plus(LOCK_DURATION));
            }
            return new Attempts(failures, null);
        });
    }

    // for tests
    void setClock(Clock clock) {
        this.clock = clock;
    }

    void resetAttempts() {
        attempts.clear();
    }
}
