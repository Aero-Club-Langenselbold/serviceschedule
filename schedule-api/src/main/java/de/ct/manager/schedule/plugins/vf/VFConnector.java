package de.ct.manager.schedule.plugins.vf;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import de.ct.manager.schedule.authentication.LoginRequest;
import de.ct.manager.schedule.user.Role;
import de.ct.manager.schedule.user.RoleRepository;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRepository;
import de.ct.manager.schedule.user.UserRole;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import lombok.extern.slf4j.Slf4j;

/*
 * This class represents the connector to the German 'vereinsflieger.de' API. It is used for data fetching and authentication when the FeatureFlag 'vfLoginFeature' is enabled.
 */
@Component
@Slf4j
public class VFConnector {

    private RestTemplate restTemplate;
    private String apiUrl;
    private String apiKey;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private CircuitBreaker circuitBreaker;

    public VFConnector(RestTemplate restTemplate, @Value("${vf.api.url}") String apiUrl, @Value("${vf.api.key}") String apiKey, UserRepository userRepository, RoleRepository roleRepository) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;

        // Configure the Circuit Breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open the circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before retrying
                .slidingWindowSize(10) // Evaluate the last 10 calls
                .build();

        this.circuitBreaker = CircuitBreaker.of("VFConnectorCircuitBreaker", config);
    }

    public String getRequestToken() {
        log.info("Requesting VF access token from {}/auth/accesstoken", apiUrl);
        // Define the API call logic
        Supplier<String> apiCall = () -> {
            VFRequestTokenResponse response = restTemplate.getForObject(apiUrl + "/auth/accesstoken", VFRequestTokenResponse.class);
            String token = response != null ? response.getAccesstoken() : null;
            log.debug("VF access token response: {}", response);
            return token;
        };

        // Wrap the API call with the Circuit Breaker
        Supplier<String> decoratedCall = Decorators.ofSupplier(apiCall)
                .withCircuitBreaker(circuitBreaker)
                .decorate();

        try {
            String token = decoratedCall.get();
            if (token == null) {
                log.warn("VF access token response was null or empty");
            } else {
                log.info("VF access token retrieved successfully (length={})", token.length());
            }
            return token;
        } catch (Exception e) {
            log.error("Failed to retrieve VF access token. Circuit breaker state: {}. Error: {}",
                    circuitBreaker.getState(), e.getMessage(), e);
            return "Fallback: API is currently unavailable";
        }
    }

    public Authentication signIn(LoginRequest loginRequest) {
        log.info("VF signIn attempt for user: {}", loginRequest.getUsername());
        String accesstoken = getRequestToken();
        if (accesstoken == null) {
            log.error("signIn aborted: access token is null");
            throw new RuntimeException("Failed to retrieve access token from VF API");
        }
        if (accesstoken.startsWith("Fallback:")) {
            log.error("signIn aborted: VF API unavailable ({})", accesstoken);
            throw new RuntimeException(accesstoken);
        }

        VFLoginRequest vfLoginRequest = new VFLoginRequest();
        vfLoginRequest.setAppkey(this.apiKey);
        vfLoginRequest.setUsername(loginRequest.getUsername());
        vfLoginRequest.setPassword(DigestUtils.md5DigestAsHex(loginRequest.getPassword().getBytes()));
        vfLoginRequest.setAuthSecret(loginRequest.getAuthSecret());
        vfLoginRequest.setAccesstoken(accesstoken);

        log.info("Sending VF signin request for user: {}", loginRequest.getUsername());
        Supplier<VFResponse> loginCall = () -> {
            return restTemplate.postForObject(apiUrl + "/auth/signin", vfLoginRequest, VFResponse.class);
        };

        Supplier<VFResponse> decoratedLoginCall = Decorators.ofSupplier(loginCall)
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        VFResponse loginResponse = decoratedLoginCall.get();
        log.debug("VF signin response status: {}", loginResponse != null ? loginResponse.getHttpstatuscode() : "null");

        if (loginResponse == null || loginResponse.getHttpstatuscode() != HttpStatus.OK.value()) {
            log.warn("VF signin failed for user: {} — HTTP status: {}", loginRequest.getUsername(),
                    loginResponse != null ? loginResponse.getHttpstatuscode() : "null");
            throw new BadCredentialsException("Invalid username or password");
        }

        VFUserResponse userResponse = getUser(accesstoken);
        log.debug("VF getUser response: email={}", userResponse != null ? userResponse.getEmail() : "null");

        if (userResponse != null && userResponse.getEmail() != null) {
            // save user to db and return user authentication
            Optional<User> user = userRepository.findByUsername(userResponse.getEmail());
            List<GrantedAuthority> authorities;
            if (!user.isPresent()) {
                log.info("VF user {} not found locally — creating new account", userResponse.getEmail());
                Role defaultRole = roleRepository.findByName(UserRole.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("Error: Role not found."));

                User newUser = new User();
                newUser.setUsername(userResponse.getEmail());
                newUser.setPassword(userResponse.getEmail());
                newUser.getRoles().add(defaultRole);
                userRepository.save(newUser);
                authorities = List.of(new SimpleGrantedAuthority(UserRole.ROLE_USER.name()));
            } else {
                log.info("VF user {} authenticated with roles: {}", userResponse.getEmail(),
                        user.get().getRoles().stream().map(r -> r.getName().name()).toList());
                authorities = user.get().getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .collect(java.util.stream.Collectors.toList());
            }

            UserDetails userDetails = new UserDetailsImpl(userResponse.getUid(),
                userResponse.getEmail(),
                vfLoginRequest.getPassword(),
                authorities
            );

            return new UsernamePasswordAuthenticationToken(userDetails, vfLoginRequest.getPassword());
        }

        log.error("VF signIn returned no user data for user: {}", loginRequest.getUsername());
        return null;
    }

    public VFUserResponse getUser(String accesstoken) {
        log.info("Requesting VF user info from {}/auth/getuser", apiUrl);
        VFRequest vfRequest = new VFRequest();
        vfRequest.setAccesstoken(accesstoken);

        Supplier<VFUserResponse> userCall = () -> {
            return restTemplate.postForObject(apiUrl + "/auth/getuser", vfRequest, VFUserResponse.class);
        };

        Supplier<VFUserResponse> decoratedUserCall = Decorators.ofSupplier(userCall)
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        try {
            VFUserResponse response = decoratedUserCall.get();
            log.debug("VF getUser raw response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to retrieve VF user info. Circuit breaker state: {}. Error: {}",
                    circuitBreaker.getState(), e.getMessage(), e);
            throw e;
        }
    }
}
