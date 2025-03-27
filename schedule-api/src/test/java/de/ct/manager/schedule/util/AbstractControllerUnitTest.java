package de.ct.manager.schedule.util;

import static org.mockito.Mockito.when;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.security.AuthEntryPointJwt;
import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.security.WebSecurityConfig;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserDetailsServiceImpl;
import de.ct.manager.schedule.user.UserRepository;
import de.ct.manager.schedule.user.UserRole;

import static de.ct.manager.schedule.util.TestUtils.*;

@Import({
    WebSecurityConfig.class,
    JwtUtils.class,
    AuthEntryPointJwt.class,
    UserDetailsServiceImpl.class
})
public abstract class AbstractControllerUnitTest {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    protected UserRepository userRepository;

    @MockitoBean
    protected ParticipantRepository participantRepository;

    @MockitoBean
    protected AuthenticationManager authenticationManager;

    public void mockValidAuthentication(UserRole role) {
        UserDto user = TestUtils.getUserForRole(role);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());

        Authentication authentication = new TestingAuthenticationToken(new UserDetailsImpl("1234", user.getUsername(), user.getPassword(), createAuthorities(role)), null);

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(createTestUser(role)));
        when(authenticationManager.authenticate(token)).thenReturn(authentication);
    }

    protected User createTestUser(UserRole role) {
        UserDto userMock = TestUtils.getUserForRole(role);

        User user = new User();
        user.setId("1234");
        user.setUsername(userMock.getUsername());
        user.setUserRole(role);
        user.setPassword(userMock.getPassword());

        return user;
    }

    protected String generateValidJwtToken(UserRole role) {
        UserDto user = TestUtils.getUserForRole(role);
        Authentication authentication = new TestingAuthenticationToken(new UserDetailsImpl("1234", user.getUsername(), user.getPassword(), createAuthorities(role)), null);

        return jwtUtils.generateJwtToken(authentication);
    }
}
