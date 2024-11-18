package de.ct.manager.schedule.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ct.manager.schedule.security.WebSecurityConfig;

@Import({
    WebSecurityConfig.class
})
public abstract class AbstractControllerUnitTest {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected MockMvc mockMvc;
}
