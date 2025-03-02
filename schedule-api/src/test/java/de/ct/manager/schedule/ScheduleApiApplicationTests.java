package de.ct.manager.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.ct.manager.schedule.security.AuthEntryPointJwt;
import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.security.WebSecurityConfig;
import de.ct.manager.schedule.user.UserDetailsServiceImpl;


@SpringBootTest
@Import({
    WebSecurityConfig.class,
    JwtUtils.class,
    AuthEntryPointJwt.class,
    UserDetailsServiceImpl.class
})
class ScheduleApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
