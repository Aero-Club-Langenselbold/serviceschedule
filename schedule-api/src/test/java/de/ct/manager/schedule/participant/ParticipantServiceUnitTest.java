package de.ct.manager.schedule.participant;

import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.ct.manager.schedule.service.ServiceRepository;

@ExtendWith(SpringExtension.class)
class ParticipantServiceUnitTest {

    @Mock
    ParticipantRepository participantRepository;

    @Mock
    ServiceRepository serviceRepository;

    @InjectMocks
    ParticipantService participantService;

    @Test
    void testLoadParticipantsPage() {
        PageRequest pr = PageRequest.of(2, 10, Direction.DESC, "name");

        when(participantRepository.findAll(pr)).thenReturn(generatePageResponse());

        Page<Participant> result = participantService.listParticipants(pr);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getNumberOfElements()).isEqualTo(10);
        Assertions.assertThat(result.hasContent()).isTrue();
    }

    @Test
    void testLoadParticipantsList() {
        when(participantRepository.findAll()).thenReturn(generateListResponse());

        List<Participant> result = participantService.listParticipants();

        Assertions.assertThat(result).isNotNull().hasSize(10);
    }

    @Test
    void testGetParticipant() {
        Participant participant = new Participant();
        participant.setFirstname("Donald");
        participant.setLastname("Duck");

        when(participantRepository.findById("1")).thenReturn(Optional.of(participant));
        when(participantRepository.findById("0")).thenReturn(Optional.empty());

        Assertions.assertThat(participantService.getParticipant("1")).isNotNull();
        Assertions.assertThat(participantService.getParticipant("0")).isNull();
    }

    @Test
    void testCreateOrUpdateParticipant() {
        Participant participant = new Participant();
        participant.setFirstname("Donald");
        participant.setLastname("Duck");

        when(participantRepository.save(participant)).thenReturn(participant);

        Assertions.assertThat(participantService.createOrUpdateParticipant(participant)).isNotNull();
    }

    @Test
    void testDeleteParticipant() {
        Participant participant = new Participant();
        participant.setFirstname("Donald");
        participant.setLastname("Duck");

        when(participantRepository.findById("1")).thenReturn(Optional.of(participant));
        when(participantRepository.findById("0")).thenReturn(Optional.empty());

        participantService.deleteParticipant("1");
        participantService.deleteParticipant("0");

        Assertions.assertThatNoException();
    }

    @Test
    void testImportParticipants() throws Exception {
        // Prepare a mock MultipartFile
        String csvContent = new String(
            Files.readAllBytes(Paths.get("src/test/resources/testimport.csv")),
            StandardCharsets.UTF_8
        );
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "testimport.csv",
            "text/csv",
            csvContent.getBytes()
        );

        // Mock the repository save behavior
        when(participantRepository.saveAll(Mockito.anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRepository.findAll()).thenReturn(new ArrayList<>());

        // Call the service method
        participantService.importParticipants(mockFile);
    }

    private Page<Participant> generatePageResponse() {
        return new PageImpl<>(generateListResponse());
    }

    private List<Participant> generateListResponse() {
        List<Participant> participants = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Participant participant = new Participant();
            participant.setFirstname("Donald");
            participant.setLastname("Duck No. " + i);
            participant.setEmail("donald.duck@angermanagementissues.com");
            participant.setDateOfBirth(new Date());
            participants.add(participant);
        }

        return participants;
    }
}
