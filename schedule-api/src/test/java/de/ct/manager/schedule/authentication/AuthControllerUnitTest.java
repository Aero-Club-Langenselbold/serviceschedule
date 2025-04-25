package de.ct.manager.schedule.authentication;

import static de.ct.manager.schedule.util.TestUtils.TEST_ADMIN;
import static de.ct.manager.schedule.util.TestUtils.TEST_ADMIN_PW;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.settings.SettingsService;
import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(AuthController.class)
public class AuthControllerUnitTest extends AbstractControllerUnitTest {

    @MockitoBean
    private SettingsService settingsService;

    @Test
    void testSignInSuccess() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        mockMvc.perform(
            post("/auth/signin")
                .content(objectMapper.writeValueAsString(new LoginRequest(TEST_ADMIN, TEST_ADMIN_PW, null, null)))
                .contentType("application/json")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.username", is(TEST_ADMIN)))
            .andExpect(jsonPath("$.id", is("1234")))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.roles", notNullValue()));
    }

    @Test
    void testSignInFailure() throws Exception {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(TEST_ADMIN, "I'm_pretty_wrong");

        when(userRepository.findByUsername(TEST_ADMIN)).thenReturn(Optional.of(createTestUser(UserRole.ROLE_ADMIN)));
        when(authenticationManager.authenticate(token)).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(
            post("/auth/signin")
                .content(objectMapper.writeValueAsString(new LoginRequest(TEST_ADMIN, "I'm_pretty_wrong", null, null)))
                .contentType("application/json")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testSignUpSuccess() throws Exception {

        when(userRepository.existsByUsername(TEST_ADMIN)).thenReturn(false);
        when(participantRepository.findByEmail(TEST_ADMIN)).thenReturn(Optional.of(createTestParticipant("4711")));

        mockMvc.perform(
            post("/auth/signup")
                .content(objectMapper.writeValueAsString(new SignupRequest(TEST_ADMIN, TEST_ADMIN_PW, "4711")))
                .contentType("application/json")
            )
            .andDo(print())
            .andExpect(status().isOk());
    }



    @Test
    void testRegistrationKeyError() throws Exception {

        when(userRepository.existsByUsername(TEST_ADMIN)).thenReturn(false);
        when(participantRepository.findByEmail(TEST_ADMIN)).thenReturn(Optional.of(createTestParticipant("4712")));

        mockMvc.perform(
            post("/auth/signup")
                .content(objectMapper.writeValueAsString(new SignupRequest(TEST_ADMIN, TEST_ADMIN_PW, "4711")))
                .contentType("application/json")
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSignUpError() throws Exception {

        when(userRepository.existsByUsername(TEST_ADMIN)).thenReturn(true);

        mockMvc.perform(
            post("/auth/signup")
                .content(objectMapper.writeValueAsString(new SignupRequest(TEST_ADMIN, TEST_ADMIN_PW, "4711")))
                .contentType("application/json")
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    private Participant createTestParticipant(String key) {
        Participant p = new Participant();
        p.setEmail(TEST_ADMIN);
        p.setRegistrationKey(key);
        return p;
    }
}
