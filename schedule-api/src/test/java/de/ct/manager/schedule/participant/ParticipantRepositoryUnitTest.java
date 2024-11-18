package de.ct.manager.schedule.participant;

import java.util.Date;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class ParticipantRepositoryUnitTest {

    @Autowired
    ParticipantRepository participantRepository;

    @BeforeEach
    void setup() {
        Participant participant = new Participant();
        participant.setId("reallyuniqueid");
        participant.setFirstname("Donald");
        participant.setLastname("Duck");
        participant.setEmail("donald.duck@angermanagementissues.com");
        participant.setDateOfBirth(new Date());
        participantRepository.save(participant);
    }

    @Test
    void findParticipant() {
        Assertions.assertThat(participantRepository.findById("reallyuniqueid")).isNotEmpty();
        Assertions.assertThat(participantRepository.findById("somethingstrange")).isEmpty();
    }

    @AfterEach
    void destroy() {
        participantRepository.deleteAll();
    }
}
