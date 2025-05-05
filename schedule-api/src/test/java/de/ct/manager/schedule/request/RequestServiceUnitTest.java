package de.ct.manager.schedule.request;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantRepository;

@ExtendWith(SpringExtension.class)
class RequestServiceUnitTest {

    @Mock
    RequestRepository requestRepository;

    @Mock
    ParticipantRepository participantRepository;

    @InjectMocks
    RequestService requestService;

    @Test
    void testLoadRequestList() {
        when(requestRepository.findAll(Sort.by("createdAt").descending())).thenReturn(generateListResponse());

        List<Request> result = requestService.getAllRequests();

        Assertions.assertThat(result).isNotNull().hasSize(10);
    }

    @Test
    void testLoadRequestPage() {
        when(requestRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(generateListResponse()));

        Page<Request> result = requestService.getRequestsByPage(PageRequest.of(0, 10));

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getContent()).hasSize(10);
    }

    @Test
    void testGetRequest() {
        Request request = new Request();
        request.setHeadline("Anger Management" );
        request.setDescription("What the world could look like if we all just got along.");

        when(requestRepository.findById("1")).thenReturn(Optional.of(request));
        when(requestRepository.findById("0")).thenReturn(Optional.empty());

        Assertions.assertThat(requestService.getRequestById("1")).isNotNull();
        Assertions.assertThat(requestService.getRequestById("0")).isNull();
    }

    @Test
    void testCreateOrUpdateRequest() {
        Request request = new Request();
        request.setHeadline("Anger Management" );
        request.setDescription("What the world could look like if we all just got along.");

        when(requestRepository.save(request)).thenReturn(request);

        Assertions.assertThat(requestService.createOrUpdateRequest(request)).isNotNull();
    }

    @Test
    void testDeletePlan() {
        Request request = new Request();
        request.setHeadline("Anger Management" );
        request.setDescription("What the world could look like if we all just got along.");

        when(requestRepository.findById("1")).thenReturn(Optional.of(request));
        when(requestRepository.findById("0")).thenReturn(Optional.empty());

        requestService.deleteRequest("1");
        requestService.deleteRequest("0");

        Assertions.assertThatNoException();
    }

    @Test
    void testGetUserRequests() {
        Request request = new Request();
        request.setHeadline("Anger Management" );
        request.setDescription("What the world could look like if we all just got along.");

        Participant participant = new Participant();
        when(participantRepository.findByEmail("McFart")).thenReturn(Optional.of(participant));
        when(requestRepository.findAllByAuthor(Sort.by("createdAt").descending(), participant)).thenReturn(generateListResponse());

        List<Request> result = requestService.getUserRequests("McFart");

        Assertions.assertThat(result).isNotNull().hasSize(10);
    }

    @Test
    void testGetUserRequestsByPage() {
        Request request = new Request();
        request.setHeadline("Anger Management" );
        request.setDescription("What the world could look like if we all just got along.");

        Participant participant = new Participant();
        when(participantRepository.findByEmail("McFart")).thenReturn(Optional.of(participant));
        when(requestRepository.findAllByAuthor(PageRequest.of(0, 10), participant)).thenReturn(new PageImpl<>(generateListResponse()));

        Page<Request> result = requestService.getUserRequestsByPage("McFart", PageRequest.of(0, 10));

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getContent()).hasSize(10);
    }

    private List<Request> generateListResponse() {
        List<Request> requests = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Request request = new Request();
            request.setHeadline("Anger Management " + i);
            request.setDescription("What the world could look like if we all just got along.");
            requests.add(request);
        }

        return requests;
    }
}
