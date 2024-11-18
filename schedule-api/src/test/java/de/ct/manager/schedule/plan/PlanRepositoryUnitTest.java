package de.ct.manager.schedule.plan;

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
class PlanRepositoryUnitTest {

    @Autowired
    PlanRepository planRepository;

    @BeforeEach
    void setup() {
        Plan plan = new Plan();
        plan.setId("reallyuniqueid");
        plan.setName("Anger Management");
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());
        planRepository.save(plan);
    }

    @Test
    void findPlan() {
        Assertions.assertThat(planRepository.findById("reallyuniqueid")).isNotEmpty();
        Assertions.assertThat(planRepository.findById("somethingstrange")).isEmpty();
    }

    @AfterEach
    void destroy() {
        planRepository.deleteAll();
    }
}
