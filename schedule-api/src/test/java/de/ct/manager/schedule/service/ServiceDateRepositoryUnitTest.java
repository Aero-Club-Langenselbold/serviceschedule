package de.ct.manager.schedule.service;

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
class ServiceDateRepositoryUnitTest {

    @Autowired
    ServiceDateRepository serviceDateRepository;

    @Autowired
    ServiceRepository serviceRepository;

    @Autowired
    PlanRepository planRepository;


    @BeforeEach
    void setup() {
        Service service = new Service();
        service.setId("reallyuniqueid");
        service.setServicename("Anger Management");
        serviceRepository.save(service);

        Plan plan = new Plan();
        plan.setId("fancyplan");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());
        plan.setName("Woah - what a plan");
        planRepository.save(plan);

        ServiceDate serviceDate = new ServiceDate();
        serviceDate.setId("myservicedate");
        serviceDate.setDate(new Date());
        serviceDate.setService(service);
        serviceDate.setPlan(plan);
        serviceDate.setMandatoryAssignees(1);
        serviceDate.setStartTime(java.time.LocalTime.of(8, 0));
        serviceDate.setEndTime(java.time.LocalTime.of(16, 0));
        serviceDateRepository.save(serviceDate);
    }

    @Test
    void findServiceDate() {
        Assertions.assertThat(serviceDateRepository.findById("myservicedate")).isNotEmpty();
        Assertions.assertThat(serviceDateRepository.findById("somethingstrange")).isEmpty();
    }

    @Test
    void addServiceDate() {
        Service service = serviceRepository.findById("reallyuniqueid").get();
        Plan plan = planRepository.findById("fancyplan").get();
        ServiceDate serviceDate = new ServiceDate();
        serviceDate.setId("anotherservicedate");
        serviceDate.setDate(new Date());
        serviceDate.setService(service);
        serviceDate.setPlan(plan);
        serviceDate.setMandatoryAssignees(1);
        serviceDate.setStartTime(java.time.LocalTime.of(8, 0));
        serviceDate.setEndTime(java.time.LocalTime.of(16, 0));
        serviceDateRepository.save(serviceDate);
        Assertions.assertThat(serviceDateRepository.findById("anotherservicedate")).isNotEmpty();
    }

    @Test
    void findServiceDateByPlan() {
        Plan plan = planRepository.findById("fancyplan").get();
        Assertions.assertThat(serviceDateRepository.findByPlan(plan)).isNotEmpty();
    }

    @AfterEach
    void destroy() {
        serviceDateRepository.deleteAll();
        serviceRepository.deleteAll();
        planRepository.deleteAll();
    }
}
