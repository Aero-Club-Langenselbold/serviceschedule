package de.ct.manager.schedule.request;

import java.util.Date;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantRepository;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class RequestRepositoryUnitTest {

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    ParticipantRepository participantRepository;

    @BeforeEach
    void setup() {
        // Create a participant to associate with the request
        Participant participant = new Participant();
        participant.setId("test-participant-id");
        participant.setEmail("mail@superhero.com");
        participant.setFirstname("Bruce");
        participant.setLastname("Wayne");
        participant.setDateOfBirth(new Date());
        participant = participantRepository.save(participant);

        Request request = new Request();
        request.setId("reallyuniqueid");
        request.setHeadline("Anger Management");
        request.setDescription("What the world could look like if we all just got along.");
        request.setStatus(RequestStatus.OPEN);
        request.setType(RequestType.BUG);
        request.setAuthor(participant);

        requestRepository.save(request);
    }

    @Test
    void findRequest() {
        Assertions.assertThat(requestRepository.findById("reallyuniqueid")).isNotEmpty();
        Assertions.assertThat(requestRepository.findById("somethingstrange")).isEmpty();
    }

    @Test
    void findAllByAuthor() {
        Assertions.assertThat(requestRepository.findAllByAuthor(Sort.by("id").descending(), null)).isEmpty();
        Assertions.assertThat(requestRepository.findAllByAuthor(Sort.by("id").descending(), participantRepository.findByEmail("mail@superhero.com").get())).isNotEmpty();
    }

    @AfterEach
    void destroy() {
        requestRepository.deleteAll();
    }
}
