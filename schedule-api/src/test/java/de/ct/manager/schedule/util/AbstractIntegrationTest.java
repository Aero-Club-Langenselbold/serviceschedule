package de.ct.manager.schedule.util;

import static de.ct.manager.schedule.util.TestUtils.createAuthorities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ct.manager.schedule.security.AuthEntryPointJwt;
import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.security.WebSecurityConfig;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserDetailsServiceImpl;
import de.ct.manager.schedule.user.UserRole;

@Import({
    WebSecurityConfig.class,
    JwtUtils.class,
    AuthEntryPointJwt.class,
    UserDetailsServiceImpl.class
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:scripts/test-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(statements = "DELETE FROM auth_user", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    protected String createURLWithPort(String path) {
        return "http://localhost:" + this.port + "/api/v1/" + path;
    }

    protected HttpHeaders createHeadersWithToken(UserRole role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (role != null) {
            UserDto user = TestUtils.getUserForRole(role);
            Authentication authentication = new TestingAuthenticationToken(new UserDetailsImpl("1234", user.getUsername(), user.getPassword(), createAuthorities(role)), null);
            headers.setBearerAuth(jwtUtils.generateJwtToken(authentication));
        }

        return headers;
    }
}
