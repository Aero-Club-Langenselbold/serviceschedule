package de.ct.manager.schedule.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(UserController.class)
@Import({
    UserService.class
})
public class UserControllerUnitTest extends AbstractControllerUnitTest {

    @Autowired
    private JwtUtils jwtUtils;

    private User createUser(String id, String username, UserRole roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User(username, "secret-hash");
        user.setId(id);
        user.setRoles(Set.of(role));
        return user;
    }

    private String bearer(UserRole role) {
        return "Bearer " + generateValidJwtToken(role);
    }

    @Test
    void testGetUsersShowsBasicDataWithoutPassword() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        User user = createUser("42", "anna@test.de", UserRole.ROLE_USER);
        user.setPermissions(Set.of(UserPermission.PERMISSION_ALARM_MANAGE));

        Participant participant = new Participant();
        participant.setEmail("anna@test.de");
        participant.setFirstname("Anna");
        participant.setLastname("Albers");

        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        when(participantRepository.findAllByEmailIn(List.of("anna@test.de"))).thenReturn(List.of(participant));

        mockMvc.perform(get("/user/page").header("Authorization", bearer(UserRole.ROLE_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].username", is("anna@test.de")))
            .andExpect(jsonPath("$.content[0].fullname", is(participant.getFullname())))
            .andExpect(jsonPath("$.content[0].participantStatus").doesNotExist())
            .andExpect(jsonPath("$.content[0].roles", contains("ROLE_USER")))
            .andExpect(jsonPath("$.content[0].permissions", contains("PERMISSION_ALARM_MANAGE")))
            .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void testGetUsersPassesSearchToRepository() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);
        when(userRepository.findAll(any(Pageable.class), any(String.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user/page?search=anna").header("Authorization", bearer(UserRole.ROLE_ADMIN)))
            .andExpect(status().isOk());

        verify(userRepository).findAll(any(Pageable.class), any(String.class));
    }

    @Test
    void testUserEndpointsAreForbiddenForNonAdmins() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        String token = bearer(UserRole.ROLE_USER);

        mockMvc.perform(get("/user/page").header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(get("/user/permissions").header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(put("/user/42/permissions").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/user/42/impersonate").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    void testGetAvailablePermissions() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        mockMvc.perform(get("/user/permissions").header("Authorization", bearer(UserRole.ROLE_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", contains("PERMISSION_SCHEDULE_MANAGE", "PERMISSION_ALARM_MANAGE")));
    }

    @Test
    void testUpdatePermissionsReplacesExistingOnes() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        User user = createUser("42", "anna@test.de", UserRole.ROLE_USER);
        user.setPermissions(Set.of(UserPermission.PERMISSION_ALARM_MANAGE));
        when(userRepository.findById("42")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        mockMvc.perform(
            put("/user/42/permissions")
                .header("Authorization", bearer(UserRole.ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"PERMISSION_SCHEDULE_MANAGE\"]")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions", contains("PERMISSION_SCHEDULE_MANAGE")));
    }

    @Test
    void testUpdatePermissionsOfUnknownUser() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);
        when(userRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(
            put("/user/nope/permissions")
                .header("Authorization", bearer(UserRole.ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]")
            )
            .andExpect(status().isNotFound());

        verify(userRepository, never()).save(any());
    }

    @Test
    void testImpersonateReturnsTokenOfTargetUser() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);
        when(userRepository.findById("42")).thenReturn(Optional.of(createUser("42", "anna@test.de", UserRole.ROLE_USER)));

        String body = mockMvc.perform(post("/user/42/impersonate").header("Authorization", bearer(UserRole.ROLE_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("anna@test.de")))
            .andExpect(jsonPath("$.roles", contains("ROLE_USER")))
            .andExpect(jsonPath("$.impersonator", is("admin@test.de")))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("token").asText();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("anna@test.de");
        assertThat(jwtUtils.getImpersonatorFromJwtToken(token)).isEqualTo("admin@test.de");
    }

    @Test
    void testCannotImpersonateYourself() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);
        when(userRepository.findById("1234")).thenReturn(Optional.of(createUser("1234", "admin@test.de", UserRole.ROLE_ADMIN)));

        mockMvc.perform(post("/user/1234/impersonate").header("Authorization", bearer(UserRole.ROLE_ADMIN)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCannotImpersonateOtherAdministrators() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);
        when(userRepository.findById("99")).thenReturn(Optional.of(createUser("99", "other@test.de", UserRole.ROLE_ADMIN)));

        mockMvc.perform(post("/user/99/impersonate").header("Authorization", bearer(UserRole.ROLE_ADMIN)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testStopImpersonationReturnsAdminSession() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);
        mockValidAuthentication(UserRole.ROLE_USER);
        when(userRepository.findById("42")).thenReturn(Optional.of(createUser("42", "test@test.com", UserRole.ROLE_USER)));

        String token = jwtUtils.generateImpersonationToken(UserDetailsImpl.build(createUser("42", "test@test.com", UserRole.ROLE_USER)), "admin@test.de");

        String body = mockMvc.perform(post("/user/impersonation/stop").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("admin@test.de")))
            .andExpect(jsonPath("$.roles", contains("ROLE_ADMIN")))
            .andExpect(jsonPath("$.impersonator", nullValue()))
            .andReturn().getResponse().getContentAsString();

        String adminToken = objectMapper.readTree(body).get("token").asText();
        assertThat(jwtUtils.getImpersonatorFromJwtToken(adminToken)).isNull();
    }

    @Test
    void testStopImpersonationWithoutImpersonation() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);

        mockMvc.perform(post("/user/impersonation/stop").header("Authorization", bearer(UserRole.ROLE_USER)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testStopImpersonationFailsWhenImpersonatorLostAdminRole() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        when(userRepository.findByUsername("demoted@test.de")).thenReturn(Optional.of(createUser("7", "demoted@test.de", UserRole.ROLE_USER)));

        String token = jwtUtils.generateImpersonationToken(UserDetailsImpl.build(createUser("42", "test@test.com", UserRole.ROLE_USER)), "demoted@test.de");

        mockMvc.perform(post("/user/impersonation/stop").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
