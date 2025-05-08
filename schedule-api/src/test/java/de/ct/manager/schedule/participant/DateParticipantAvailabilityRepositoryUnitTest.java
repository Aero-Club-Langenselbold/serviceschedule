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

import de.ct.manager.schedule.plan.Plan;
import de.ct.manager.schedule.plan.PlanRepository;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class DateParticipantAvailabilityRepositoryUnitTest {
    @Autowired
    DateParticipantAvailabilityRepository dateParticipantAvailabilityRepository;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    PlanRepository planRepository;

    private Plan plan;
    private Participant participant;
    private Date date = new Date();

    @BeforeEach
    void setup() {
        this.participant = new Participant();
        this.participant.setId("myparticipant");
        this.participant.setFirstname("John");
        this.participant.setLastname("Doe");
        this.participant.setEmail("testme@tryit.com");
        this.participant.setDateOfBirth(new Date());
        this.participant.setStatus(Status.ACTIVE);
        this.participant = participantRepository.save(participant);

        this.plan = new Plan();
        this.plan.setId("fancyplan");
        this.plan.setDateFrom(new Date());
        this.plan.setDateUntil(new Date());
        this.plan.setName("Woah - what a plan");
        this.plan = planRepository.save(this.plan);

        DateParticipantAvailability dateParticipantAvailability = new DateParticipantAvailability();
        dateParticipantAvailability.setId("mydateparticipantavailability");
        dateParticipantAvailability.setPlan(this.plan);
        dateParticipantAvailability.setParticipant(this.participant);
        dateParticipantAvailability.setDate(this.date);
        dateParticipantAvailabilityRepository.save(dateParticipantAvailability);
    }

    @Test
    void findDateAvailability() {
        Assertions.assertThat(dateParticipantAvailabilityRepository.findById("mydateparticipantavailability")).isNotEmpty();
        Assertions.assertThat(dateParticipantAvailabilityRepository.findById("somethingstrange")).isEmpty();
    }

    @Test
    void addDateAvailability() {
        DateParticipantAvailability dateParticipantAvailability = new DateParticipantAvailability();
        dateParticipantAvailability.setId("wonderfulavailability");
        dateParticipantAvailability.setPlan(this.plan);
        dateParticipantAvailability.setParticipant(this.participant);
        dateParticipantAvailability.setDate(new Date());
        dateParticipantAvailabilityRepository.save(dateParticipantAvailability);

        Assertions.assertThat(dateParticipantAvailabilityRepository.findById("wonderfulavailability")).isNotEmpty();
    }

    @Test
    void findDateAvailabilityByPlan() {
        Assertions.assertThat(dateParticipantAvailabilityRepository.findByPlan(this.plan)).isNotEmpty();
    }

    @AfterEach
    void destroy() {
        dateParticipantAvailabilityRepository.deleteAll();
        participantRepository.deleteAll();
        planRepository.deleteAll();
    }
}
