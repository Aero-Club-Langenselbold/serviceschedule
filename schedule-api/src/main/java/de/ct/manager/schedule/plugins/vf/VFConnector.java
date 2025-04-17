package de.ct.manager.schedule.plugins.vf;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import de.ct.manager.schedule.authentication.LoginRequest;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserRepository;
import de.ct.manager.schedule.user.UserRole;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;

/*
 * This class represents the connector to the German 'vereinsflieger.de' API. It is used for data fetching and authentication when the FeatureFlag 'vfLoginFeature' is enabled.
 */
@Component
public class VFConnector {

    private RestTemplate restTemplate;
    private String apiUrl;
    private String apiKey;
    private UserRepository userRepository;
    private CircuitBreaker circuitBreaker;


    public VFConnector(RestTemplate restTemplate, @Value("vf.api.url") String apiUrl, @Value("vf.api.key") String apiKey, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.userRepository = userRepository;

        // Configure the Circuit Breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open the circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before retrying
                .slidingWindowSize(10) // Evaluate the last 10 calls
                .build();

        this.circuitBreaker = CircuitBreaker.of("VFConnectorCircuitBreaker", config);
    }

    public String getRequestToken() {
        // Define the API call logic
        Supplier<String> apiCall = () -> {
            VFRequestTokenResponse response = restTemplate.getForObject(apiUrl + "/auth/accesstoken", VFRequestTokenResponse.class);
            return response != null ? response.getAccesstoken() : null;
        };

        // Wrap the API call with the Circuit Breaker
        Supplier<String> decoratedCall = Decorators.ofSupplier(apiCall)
                .withCircuitBreaker(circuitBreaker)
                .decorate();

        try {
            return decoratedCall.get();
        } catch (Exception e) {
            return "Fallback: API is currently unavailable";
        }
    }

    public Authentication signIn(LoginRequest loginRequest) {
        String accesstoken = getRequestToken();
        if (accesstoken == null) {
            throw new RuntimeException("Failed to retrieve access token from VF API");
        }

        VFLoginRequest vfLoginRequest = new VFLoginRequest();
        vfLoginRequest.setAppkey(this.apiKey);
        vfLoginRequest.setUsername(loginRequest.getUsername());
        vfLoginRequest.setPassword(loginRequest.getPassword());
        vfLoginRequest.setAccesstoken(accesstoken);

        Supplier<VFResponse> loginCall = () -> {
            return restTemplate.postForObject(apiUrl + "/auth/signin", vfLoginRequest, VFResponse.class);
        };

        Supplier<VFResponse> decoratedLoginCall = Decorators.ofSupplier(loginCall)
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        if (decoratedLoginCall.get().getHttpstatuscode() != HttpStatus.OK.value()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        VFUserResponse userResponse = getUser(accesstoken);

        if (userResponse != null && userResponse.getEmail() != null) {
            // save user to db and return user authentication
            Optional<User> user = userRepository.findByUsername(userResponse.getEmail());
            if (!user.isPresent()) {
                User newUser = new User();
                newUser.setUsername(userResponse.getEmail());
                newUser.setPassword(userResponse.getEmail());
                newUser.setId(UUID.randomUUID().toString());
                newUser.setUserRole(UserRole.ROLE_USER);
                newUser.setVersion(0);
                userRepository.save(newUser);
            }

            return new UsernamePasswordAuthenticationToken(userResponse.getEmail(), null);
        }

        return null;
    }

    public VFUserResponse getUser(String accesstoken) {
        VFRequest vfRequest = new VFRequest();
        vfRequest.setAccesstoken(accesstoken);

        Supplier<VFUserResponse> userCall = () -> {
            return restTemplate.postForObject(apiUrl + "/auth/getuser", vfRequest, VFUserResponse.class);
        };

        Supplier<VFUserResponse> decoratedUserCall = Decorators.ofSupplier(userCall)
            .withCircuitBreaker(circuitBreaker)
            .decorate();

        return decoratedUserCall.get();
    }
}
