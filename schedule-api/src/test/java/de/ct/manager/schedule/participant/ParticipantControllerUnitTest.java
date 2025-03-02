package de.ct.manager.schedule.participant;

import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(ParticipantController.class)
@Import({
    ParticipantService.class
})
class ParticipantControllerUnitTest extends AbstractControllerUnitTest {

    @MockitoBean
    private ParticipantRepository participantRepository;

    @Test
    void testPageParticipants() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        PageRequest pr = PageRequest.of(2, 10, Direction.DESC, "name");

        when(participantRepository.findAll(pr)).thenReturn(generatePageResponse());

        mockMvc.perform(
            get("/participant/page")
                .param("page", "2")
                .param("size", "10")
                .param("sort", "name,desc")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(100)))
            .andExpect(jsonPath("$.content[0].lastname", is("Duck No. 0")));
    }

    @Test
    void testListParticipants() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        when(participantRepository.findAll()).thenReturn(generateListResponse());

        mockMvc.perform(
            get("/participant/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].lastname", is("Duck No. 0")));
    }

    @Test
    void testParticipant() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        when(participantRepository.findById("4711")).thenReturn(Optional.of(generateParticipant(0)));

        mockMvc.perform(
            get("/participant/fetch/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.firstname", is("Donald")))
            .andExpect(jsonPath("$.lastname", is("Duck No. 0")));
    }

    @Test
    void testCreateParticipant() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        Participant participant = generateParticipant(0);

        when(participantRepository.save(participant)).thenReturn(participant);

        mockMvc.perform(
            post("/participant/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(participant))
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.firstname", is("Donald")))
            .andExpect(jsonPath("$.lastname", is("Duck No. 0")));
    }

    @Test
    void testDeleteParticipant() throws Exception {
        mockValidAuthentication(UserRole.ROLE_USER);
        mockMvc.perform(
            delete("/participant/delete/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_USER))
            )
            .andDo(print())
            .andExpect(status().isOk());
    }

    private Page<Participant> generatePageResponse() {
        return new PageImpl<>(generateListResponse(), PageRequest.of(2, 10, Direction.DESC, "name"), 100);
    }

    private List<Participant> generateListResponse() {
        List<Participant> participants = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            participants.add(generateParticipant(i));
        }

        return participants;
    }

    private Participant generateParticipant(int index) {
        Participant participant = new Participant();
        participant.setFirstname("Donald");
        participant.setLastname("Duck No. " + index);
        participant.setEmail("donald.duck@angermanagementissues.com");
        participant.setDateOfBirth(new Date());

        return participant;
    }
}
