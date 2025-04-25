package de.ct.manager.schedule.authentication;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.ResourceAccessException;

import de.ct.manager.schedule.util.AbstractIntegrationTest;

@SuppressWarnings("null") // null warnings in assertions are false-positives
class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Sql(scripts = "classpath:scripts/test-registration-key.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "DELETE FROM participant;", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testSignUpAndLoginSuccess() {

        SignupRequest signupRequest = new SignupRequest("test@test.com", "Hallo", "4711");
        HttpEntity<SignupRequest> signUpEntity = new HttpEntity<>(signupRequest, createHeadersWithToken(null));
        restTemplate.exchange(createURLWithPort("auth/") + "signup", HttpMethod.POST, signUpEntity, new ParameterizedTypeReference<String>() {});

        LoginRequest loginRequest = new LoginRequest("test@test.com", "Hallo", null, null);
        HttpEntity<LoginRequest> loginEntity = new HttpEntity<>(loginRequest, createHeadersWithToken(null));
        ResponseEntity<JwtResponse> signInResponse = restTemplate.exchange(createURLWithPort("auth/") + "signin", HttpMethod.POST, loginEntity, new ParameterizedTypeReference<JwtResponse>() {});
        JwtResponse jwtResponse = signInResponse.getBody();

        Assertions.assertThat(jwtResponse).isNotNull();
        Assertions.assertThat(jwtResponse.getId()).isNotNull();
        Assertions.assertThat(jwtResponse.getUsername()).isEqualTo("test@test.com");
        Assertions.assertThat(jwtResponse.getRoles()).contains("ROLE_USER");
        Assertions.assertThat(jwtResponse.getToken()).isNotEmpty();
    }

    @Test
    void testSignUpUserExists() {
        SignupRequest signupRequest = new SignupRequest("test@test.com", "Hallo", "4711");
        HttpEntity<SignupRequest> signUpEntity = new HttpEntity<>(signupRequest, createHeadersWithToken(null));
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("auth/") + "signup", HttpMethod.POST, signUpEntity, new ParameterizedTypeReference<String>() {});

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testLoginFails() {
        LoginRequest loginRequest = new LoginRequest("test@test.com", "Hallo_again", null, null);
        HttpEntity<LoginRequest> loginEntity = new HttpEntity<>(loginRequest, createHeadersWithToken(null));

        try {
            ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("auth/") + "signin", HttpMethod.POST, loginEntity, new ParameterizedTypeReference<String>() {});
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        } catch (ResourceAccessException e) {
            Assertions.assertThat(e.getMessage()).isNotEmpty();
            return;
        }
    }
}

