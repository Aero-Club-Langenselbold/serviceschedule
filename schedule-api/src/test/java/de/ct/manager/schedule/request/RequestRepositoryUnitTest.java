package de.ct.manager.schedule.request;

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
class RequestRepositoryUnitTest {

    @Autowired
    RequestRepository requestRepository;

    @BeforeEach
    void setup() {
        Request request = new Request();
        request.setId("reallyuniqueid");
        request.setHeadline("Anger Management");
        request.setDescription("What the world could look like if we all just got along.");
        request.setStatus(RequestStatus.OPEN);
        request.setType(RequestType.BUG);

        requestRepository.save(request);
    }

    @Test
    void findRequest() {
        Assertions.assertThat(requestRepository.findById("reallyuniqueid")).isNotEmpty();
        Assertions.assertThat(requestRepository.findById("somethingstrange")).isEmpty();
    }

    @AfterEach
    void destroy() {
        requestRepository.deleteAll();
    }
}
