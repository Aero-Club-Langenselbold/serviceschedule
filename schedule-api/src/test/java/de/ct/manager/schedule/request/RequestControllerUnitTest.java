package de.ct.manager.schedule.request;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(RequestController.class)
@Import({
    RequestService.class
})
public class RequestControllerUnitTest extends AbstractControllerUnitTest {

    @MockitoBean
    private RequestRepository requestRepository;

    @Test
    void testListRequests() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        when(requestRepository.findAll()).thenReturn(generateListResponse());

        mockMvc.perform(
            get("/request/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].headline", is("Anger Management 0")));
    }

    @Test
    void testPageRequests() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);
        PageRequest pr = PageRequest.of(2, 10, Direction.DESC, "headline");

        when(requestRepository.findAll(pr)).thenReturn(generatePageResponse());

        mockMvc.perform(
            get("/request/page")
                .param("page", "2")
                .param("size", "10")
                .param("sort", "headline,desc")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(100)))
            .andExpect(jsonPath("$.content[0].headline", is("Anger Management 0")));
    }

    @Test
    void testRequest() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        when(requestRepository.findById("4711")).thenReturn(Optional.of(generateRequest(0)));

        mockMvc.perform(
            get("/request/fetch/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.headline", is("Anger Management 0")));
    }

    @Test
    void testCreateRequest() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        Request request = generateRequest(0);

        when(requestRepository.save(request)).thenReturn(request);

        mockMvc.perform(
            post("/request/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.headline", is("Anger Management 0")));
    }

    @Test
    void testDeleteRequest() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        mockMvc.perform(
            delete("/request/delete/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk());
    }

    private Page<Request> generatePageResponse() {
        return new PageImpl<>(generateListResponse(), PageRequest.of(2, 10, Direction.DESC, "name"), 100);
    }

    private List<Request> generateListResponse() {
        List<Request> requests = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            requests.add(generateRequest(i));
        }

        return requests;
    }

    private Request generateRequest(int index) {
        Request request = new Request();
        request.setHeadline("Anger Management " + index);
        request.setDescription("What the world could look like if we all just got along.");

        return request;
    }
}
