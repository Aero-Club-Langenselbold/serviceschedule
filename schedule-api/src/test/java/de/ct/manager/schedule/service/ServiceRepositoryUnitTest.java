package de.ct.manager.schedule.service;

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
class ServiceRepositoryUnitTest {

    @Autowired
    ServiceRepository serviceRepository;

    @BeforeEach
    void setup() {
        Service service = new Service();
        service.setId("reallyuniqueid");
        service.setServicename("Anger Management");
        serviceRepository.save(service);
    }

    @Test
    void findService() {
        Assertions.assertThat(serviceRepository.findById("reallyuniqueid")).isNotEmpty();
        Assertions.assertThat(serviceRepository.findById("somethingstrange")).isEmpty();
    }

    @AfterEach
    void destroy() {
        serviceRepository.deleteAll();
    }
}
