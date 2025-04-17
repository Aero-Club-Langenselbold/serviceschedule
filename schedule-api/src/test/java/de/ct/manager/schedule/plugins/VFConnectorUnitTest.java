package de.ct.manager.schedule.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ct.manager.schedule.authentication.LoginRequest;
import de.ct.manager.schedule.plugins.vf.VFConnector;
import de.ct.manager.schedule.plugins.vf.VFRequestTokenResponse;
import de.ct.manager.schedule.plugins.vf.VFResponse;
import de.ct.manager.schedule.plugins.vf.VFUserResponse;
import de.ct.manager.schedule.security.JacksonConfig;
import de.ct.manager.schedule.user.UserRepository;

@ExtendWith(SpringExtension.class)
public class VFConnectorUnitTest {

    private static final String VF_API_URL = "http://api.vflocal.de/rest";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserRepository userRepository;

    private VFConnector vfConnector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        vfConnector = new VFConnector(restTemplate, VF_API_URL, "4711", userRepository);
        objectMapper = new JacksonConfig().objectMapper();
    }

    @Test
    void testRequestToken() throws Exception {
        Path filePath = Paths.get("src/test/resources/plugins/vf/request_token_response.json");
        String mockResponseJson = Files.readString(filePath);

        VFRequestTokenResponse mockResponse = objectMapper.readValue(mockResponseJson, VFRequestTokenResponse.class);

        when(restTemplate.getForObject(VF_API_URL + "/auth/accesstoken", VFRequestTokenResponse.class)).thenReturn(mockResponse);

        // Call the method
        String response = vfConnector.getRequestToken();

        // Assert the response
        assertEquals("2cb11f03b89a46532c34e2293eac50f6fbebf460581020d4c19e95e9b84a8d4d", response);
        verify(restTemplate, times(1)).getForObject(VF_API_URL + "/auth/accesstoken", VFRequestTokenResponse.class);
    }

    @Test
    void testSignInSuccess() throws Exception {
        Path tokenPath = Paths.get("src/test/resources/plugins/vf/request_token_response.json");
        String mockTokenResponseJson = Files.readString(tokenPath);
        VFRequestTokenResponse mockTokenResponse = objectMapper.readValue(mockTokenResponseJson, VFRequestTokenResponse.class);
        when(restTemplate.getForObject(VF_API_URL + "/auth/accesstoken", VFRequestTokenResponse.class)).thenReturn(mockTokenResponse);

        VFResponse mockResponse = new VFResponse();
        mockResponse.setHttpstatuscode(HttpStatus.OK.value());
        when(restTemplate.postForObject(eq(VF_API_URL + "/auth/signin"), any(), eq(VFResponse.class))).thenReturn(mockResponse);

        Path userPath = Paths.get("src/test/resources/plugins/vf/request_user_response.json");
        String mockUserResponseJson = Files.readString(userPath);
        VFUserResponse mockUserResponse = objectMapper.readValue(mockUserResponseJson, VFUserResponse.class);
        when(restTemplate.postForObject(eq(VF_API_URL + "/auth/getuser"), any(), eq(VFUserResponse.class))).thenReturn(mockUserResponse);

        LoginRequest lr = new LoginRequest();
        lr.setUsername("testuser");
        lr.setPassword("testpassword");
        Authentication auth = vfConnector.signIn(lr);

        assertEquals("max.powers@worlddominationtour.com", ((UserDetails) auth.getPrincipal()).getUsername());
        verify(restTemplate, times(1)).getForObject(VF_API_URL + "/auth/accesstoken", VFRequestTokenResponse.class);
        verify(restTemplate, times(1)).postForObject(eq(VF_API_URL + "/auth/signin"), any(), eq(VFResponse.class));
        verify(restTemplate, times(1)).postForObject(eq(VF_API_URL + "/auth/getuser"), any(), eq(VFUserResponse.class));
    }
}
