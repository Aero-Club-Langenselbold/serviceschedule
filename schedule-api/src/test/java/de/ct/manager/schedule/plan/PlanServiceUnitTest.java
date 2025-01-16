package de.ct.manager.schedule.plan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantAssignment;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.participant.Status;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import de.ct.manager.schedule.service.ServiceRepository;
import lombok.Data;

@ExtendWith(SpringExtension.class)
class PlanServiceUnitTest {

    @Mock
    PlanRepository planRepository;

    @Mock
    ServiceRepository serviceRepository;

    @Mock
    ParticipantRepository participantRepository;

    @InjectMocks
    PlanService planService;

    @Test
    void testLoadPlanList() {
        when(planRepository.findAll()).thenReturn(generateListResponse());

        List<Plan> result = planService.listPlans();

        Assertions.assertThat(result).isNotNull().hasSize(10);
    }

    @Test
    void testGetPlan() {
        Plan plan = new Plan();
        plan.setName("Anger Management");
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        when(planRepository.findById("1")).thenReturn(Optional.of(plan));
        when(planRepository.findById("0")).thenReturn(Optional.empty());

        Assertions.assertThat(planService.getPlan("1")).isNotNull();
        Assertions.assertThat(planService.getPlan("0")).isNull();
    }

    @Test
    void testCreateOrUpdatePlan() {
        Plan plan = new Plan();
        plan.setName("Anger Management");
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        when(planRepository.save(plan)).thenReturn(plan);

        Assertions.assertThat(planService.createOrUpdatePlan(plan)).isNotNull();
    }

    @Test
    void testDeleteParticipant() {
        Plan plan = new Plan();
        plan.setName("Anger Management");
        plan.setDescription("What the world could look like if we all just got along.");
        plan.setDateFrom(new Date());
        plan.setDateUntil(new Date());

        when(planRepository.findById("1")).thenReturn(Optional.of(plan));
        when(planRepository.findById("0")).thenReturn(Optional.empty());

        planService.deletePlan("1");
        planService.deletePlan("0");

        Assertions.assertThatNoException();
    }

    @Test
    void testGeneratePlanWithoutParticipants() {
        // result should be an empty plan
        PlanDummyData dummyData = new PlanDummyData();

        when(planRepository.findById(dummyData.getPlan().getId())).thenReturn(Optional.of(dummyData.getPlan()));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        // participants without availabilities
        List<ParticipantAssignment> participantAvailability = new ArrayList<>();
        for (ParticipantAssignment pa : dummyData.getParticipantAvailability()) {
            participantAvailability.add(new ParticipantAssignment(pa.getParticipant(), new ArrayList<>()));
        }

        Plan plan = planService.generatePlan(dummyData.getPlan().getId(),
            dummyData.getServiceDates(),
            participantAvailability);

        Assertions.assertThat(plan).isNotNull();
        Assertions.assertThat(plan.getEntries()).isEmpty();
    }

    @Test
    void testGeneratePlan() {
        Plan plan = generatePlan();

        Assertions.assertThat(plan).isNotNull();

        // TODO: Verify: Participants under the age of 18 may only be assigned to services with another adult
        // TODO: Verify: If there are more services than participants all participants must have at least one assignment
        // TODO: consider the date of the assignment and the current age of the participant
        // TODO: verify that each Participant has roughly the same number of assignments for each service he is assigned to
        // TODO: verify that on days with two participants for the same service, two participants are assigned
    }

    @Test
    void testPlanHasAllDatesCovered() {
        Plan plan = generatePlan();

        // plan has 52 assignable dates and 2 services. One Service has 2 participants on Sundays (Total of 26)
        // So needs to have 130 plan entries
        // also verifies that every service has an assignment for each date
        Assertions.assertThat(plan.getEntries()).isNotNull().hasSize(130);
    }

    @Test
    void testParticipantSingleServicePerDay() {
        Plan plan = generatePlan();

        // verify that that one participant may not have an entry for multiple servies on the same day
        for (PlanEntry entry : plan.getEntries()) {
            for (PlanEntry nextEntry : plan.getEntries()) {
                if (!entry.equals(nextEntry)
                    && entry.getParticipant().equals(nextEntry.getParticipant())
                    && entry.getStartTime().equals(nextEntry.getStartTime())) {
                    Assertions.fail("Participant " + entry.getParticipant().getFirstname() + " has entries for multiple services on the same day");
                }
            }
        }
    }

    @Test
    void testThresholdViolations() {
        Plan plan = generatePlan();

        // verify that every participant has at least 3 weeks between assignments if there are enough participants
        int thresholdViolations = 0;
        List<Participant> checkedParticipants = new ArrayList<>();

        for (PlanEntry planEntry : plan.getEntries()) {
            if (checkedParticipants.contains(planEntry.getParticipant())) {
                continue;
            }
            List<LocalDate> assignedDates = new ArrayList<>();
            assignedDates.add(planEntry.getStartTime().toLocalDate());
            for (PlanEntry checkEntry : plan.getEntries()) {
                if (!planEntry.equals(checkEntry) && planEntry.getParticipant().equals(checkEntry.getParticipant())) {
                    assignedDates.add(checkEntry.getStartTime().toLocalDate());
                }
            }
            assignedDates.sort(Comparator.naturalOrder());
            for (int i = 0; i < assignedDates.size() - 1; i++) {
                LocalDate current = assignedDates.get(i);
                LocalDate next = assignedDates.get(i + 1);
                if (ChronoUnit.WEEKS.between(current, next) < 3) {
                    thresholdViolations++;
                }
            }

            checkedParticipants.add(planEntry.getParticipant());
        }
        if (thresholdViolations > 5) {
            Assertions.fail("There are too many violations of the 3 week threshold between assignments (" + thresholdViolations + ")");
        }
    }

    @Test
    void testParticipantAssignmentCounts() {
        Plan plan = generatePlan();

        HashMap<Participant, Integer> participantAssignments = new HashMap<>();
        for (PlanEntry planEntry : plan.getEntries()) {
            if (participantAssignments.containsKey(planEntry.getParticipant())) {
                participantAssignments.put(planEntry.getParticipant(), participantAssignments.get(planEntry.getParticipant()) + 1);
            } else {
                participantAssignments.put(planEntry.getParticipant(), 1);
            }
        }
        // john dow numbers 7, 8, 15, 21, 22, 23 should have roughly the same number of assignments
        Map<Participant, Integer> filteredParticipantAssignments = participantAssignments.entrySet().stream()
            .filter(entry -> entry.getKey().getFirstname().endsWith(".7") ||
                    entry.getKey().getFirstname().endsWith(".8") ||
                    entry.getKey().getFirstname().endsWith(".15") ||
                    entry.getKey().getFirstname().endsWith(".21") ||
                    entry.getKey().getFirstname().endsWith(".22") ||
                    entry.getKey().getFirstname().endsWith(".23") ||
                    entry.getKey().getFirstname().endsWith(".30") ||
                    entry.getKey().getFirstname().endsWith(".31") ||
                    entry.getKey().getFirstname().endsWith(".32") ||
                    entry.getKey().getFirstname().endsWith(".33") ||
                    entry.getKey().getFirstname().endsWith(".34"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int totalAssignments = filteredParticipantAssignments.values().stream().mapToInt(Integer::intValue).sum();
        int numberOfParticipants = filteredParticipantAssignments.size();
        double averageAssignments = (double) totalAssignments / numberOfParticipants;

        // Verify that each participant's assignment count is within +/-1 of the average
        filteredParticipantAssignments.forEach((participant, count) -> {
            Assertions.assertThat(count).isBetween((int) Math.floor(averageAssignments) - 1, (int) Math.ceil(averageAssignments) + 1);
        });
    }

    @Test
    void testMinorParticipantsNotAssignedSolo() {
        Plan plan = generatePlan();

        for (PlanEntry planEntry : plan.getEntries()) {
            System.out.println(planEntry.toString());
        }

        // Verify that minors are never assigned to services alone
        for (PlanEntry planEntry : plan.getEntries()) {
            if (planEntry.getParticipant().getAgeAtDate(planEntry.getStartTime().toLocalDate()) < 18 && !hasAdult(planEntry.getService(), planEntry.getStartTime(), plan.getEntries())) {
                Assertions.fail("Minor participant " + planEntry.getParticipant().getFirstname() + " is not assigned to a service with another an adult");
            }
        }
    }

    private Plan generatePlan() {
        PlanDummyData dummyData = new PlanDummyData();

        when(planRepository.findById(dummyData.getPlan().getId())).thenReturn(Optional.of(dummyData.getPlan()));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        return planService.generatePlan(dummyData.getPlan().getId(),
            dummyData.getServiceDates(),
            dummyData.getParticipantAvailability());
    }

    private List<Plan> generateListResponse() {
        List<Plan> plans = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Plan plan = new Plan();
            plan.setName("Anger Management " + i);
            plan.setDescription("What the world could look like if we all just got along.");
            plan.setDateFrom(new Date());
            plan.setDateUntil(new Date());
            plans.add(plan);
        }

        return plans;
    }

    private boolean hasAdult(Service service, LocalDateTime date, Set<PlanEntry> entries) {
        for (PlanEntry entry : entries) {
            if (entry.getStartTime().equals(date) && entry.getService().equals(service) && entry.getParticipant().getAgeAtDate(date.toLocalDate()) >= 18) {
                return true;
            }
        }
        return false;
    }

    @Data
    private class PlanDummyData {
        private Plan plan;
        private List<ServiceDate> serviceDates;
        private List<Service> services;
        private List<Date> assignableDates;
        private List<ParticipantAssignment> participantAvailability;

        public PlanDummyData() {
            generateDummyData();
        }

        private void generateDummyData() {
            generateDummyPlan();
            generateDummyServices();
            generateDummyDates();
            generateDummyServiceDates();

            assignDummyAvailabilities(generateDummyParticipants());
        }

        private void generateDummyPlan() {
            this.plan = new Plan();
            this.plan.setName("Anger Management");
            this.plan.setDescription("What the world could look like if we all just got along.");
            this.plan.setDateFrom(new Date());
            this.plan.setDateUntil(new Date());
        }

        private void generateDummyServiceDates() {
            this.serviceDates = new ArrayList<>();

            for (Date date : this.assignableDates) {
                for (Service service : this.services) {
                    // the "fancy funky service" requires 2 participants on sundays
                    if (service.getServicename().equals("Fancy Funky Service") && date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
                        this.serviceDates.add(new ServiceDate(date, service, 2));
                    } else {
                        this.serviceDates.add(new ServiceDate(date, service, 1));
                    }
                }
            }
        }

        private void generateDummyServices() {
            this.services = new ArrayList<>();

            Service service1 = new Service();
            service1.setServicename("Fancy Funky Service");
            service1.setParticipants(null);

            Service service2 = new Service();
            service2.setServicename("Not so Funky Service");
            service2.setParticipants(null);

            this.services.add(service1);
            this.services.add(service2);
        }

        private void generateDummyDates() {
            this.assignableDates = new ArrayList<>();

            // Assign every Saturday and Sunday for 6 months starting January 2020
            LocalDate startDate = LocalDate.of(2020, 1, 1);
            LocalDate endDate = startDate.plusMonths(6);

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    assignableDates.add(Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
                currentDate = currentDate.plusDays(1);
            }

            // should be 26 Saturdays and 26 Sundays
            Assertions.assertThat(this.assignableDates).hasSize(52);
        }

        private List<Participant> generateDummyParticipants() {
            List<Participant> participants = new ArrayList<>();

            for (int i = 0; i < 35; i++) {
                Participant participant = new Participant();
                participant.setFirstname("John." + i);
                participant.setLastname("Doe");
                participant.setStatus(Status.ACTIVE);
                participant.setEmail("john.doe." + i + "@funwithflags.com");
                // create 5 minors (under the age of 18): Birthyear starting 13 years before 2020 increasing by 1 year each loop
                participant.setDateOfBirth(Date.from(LocalDate.of(2020, Month.MARCH, 1).minusYears(13 + i).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                participants.add(participant);
            }

            return participants;
        }

        private void assignDummyAvailabilities(List<Participant> participants) {
            // NOTE: For deterministic test results no random assignments are made
            participantAvailability = new ArrayList<>();

            // assign services to participants
            for (int i = 0;i < 5;i++) {
                // minors are only assigned to one service
                participants.get(i).getServices().add(services.get(0));
            }
            for (int i = 5;i < 12;i++) {
                // some adults are assigned to both services
                participants.get(i).getServices().add(services.get(0));
                participants.get(i).getServices().add(services.get(1));
            }
            for (int i = 12;i < 24;i++) {
                // some adults are assigned to the first service only
                participants.get(i).getServices().add(services.get(0));
            }
            for (int i = 24;i < 35;i++) {
                // some adults are assigned to the second service only
                participants.get(i).getServices().add(services.get(1));
            }

            // every sunday
            List<Date> assignableSundays = assignableDates.stream()
                .filter(j -> j.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY)
                .toList();
            Assertions.assertThat(assignableSundays).hasSize(26);
            // every saturday
            List<Date> assignableSaturdays = assignableDates.stream()
                .filter(j -> j.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY)
                .toList();
            Assertions.assertThat(assignableSaturdays).hasSize(26);

            // REMINDER: participants 0 - 4 are minors
            // assign dates to participants
            participantAvailability.add(new ParticipantAssignment(participants.get(0), assignableDates)); // particiant 0 can cover all dates
            participantAvailability.add(new ParticipantAssignment(participants.get(1), assignableSundays)); // particiant 1 can cover every sunday
            participantAvailability.add(new ParticipantAssignment(participants.get(2), assignableSaturdays)); // particiant 2 can cover every saturday
            participantAvailability.add(new ParticipantAssignment(participants.get(3), assignableSaturdays.subList(0, 3))); // particiant 3 can cover 3 saturdays
            participantAvailability.add(new ParticipantAssignment(participants.get(4), new ArrayList<>())); // particiant 4 cannot cover anything

            participantAvailability.add(new ParticipantAssignment(participants.get(5), assignableDates)); // particiant 5 can cover all dates
            participantAvailability.add(new ParticipantAssignment(participants.get(6), assignableDates)); // particiant 6 can cover all dates
            participantAvailability.add(new ParticipantAssignment(participants.get(7), assignableSundays)); // particiant 7 can cover every sunday
            participantAvailability.add(new ParticipantAssignment(participants.get(8), assignableSaturdays)); // particiant 8 can cover every saturday
            participantAvailability.add(new ParticipantAssignment(participants.get(9), assignableSaturdays.subList(10, 12))); // particiant 9 can cover 2 saturdays
            participantAvailability.add(new ParticipantAssignment(participants.get(10), assignableDates.subList(42, 52))); // particiant 10 can cover the last 10 dates
            participantAvailability.add(new ParticipantAssignment(participants.get(11), assignableDates.subList(0, 20))); // particiant 11 can cover the first 20 dates
            participantAvailability.add(new ParticipantAssignment(participants.get(12), assignableDates.subList(15, 43))); // particiant 12 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(13), assignableDates.subList(10, 50))); // particiant 13 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(14), assignableDates.subList(5, 30))); // particiant 14 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(15), assignableDates)); // particiant 15 can cover all dates
            participantAvailability.add(new ParticipantAssignment(participants.get(16), assignableDates.subList(10, 11))); // particiant 16 can cover only one date
            participantAvailability.add(new ParticipantAssignment(participants.get(17), assignableDates.subList(19, 20))); // particiant 17 can cover only one date
            participantAvailability.add(new ParticipantAssignment(participants.get(18), assignableDates.subList(5, 6))); // particiant 18 can cover only one date
            participantAvailability.add(new ParticipantAssignment(participants.get(19), assignableDates.subList(42, 43))); // particiant 19 can cover only one date
            participantAvailability.add(new ParticipantAssignment(participants.get(20), assignableDates.subList(38, 41))); // particiant 20 can cover only 3 dates
            participantAvailability.add(new ParticipantAssignment(participants.get(21), assignableSundays)); // particiant 21 can cover every sunday
            participantAvailability.add(new ParticipantAssignment(participants.get(22), assignableSaturdays)); // particiant 22 can cover every saturday
            participantAvailability.add(new ParticipantAssignment(participants.get(23), assignableDates)); // particiant 23 can cover all dates
            participantAvailability.add(new ParticipantAssignment(participants.get(24), assignableDates.subList(0, 35))); // particiant 24 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(25), assignableDates.subList(12, 52))); // particiant 25 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(26), assignableDates.subList(32, 52))); // particiant 26 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(27), assignableDates.subList(8, 27))); // particiant 27 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(28), assignableDates.subList(40, 45))); // particiant 28 can cover some dates
            participantAvailability.add(new ParticipantAssignment(participants.get(29), assignableDates.subList(3, 16))); // particiant 29 can cover lots of dates
            participantAvailability.add(new ParticipantAssignment(participants.get(30), assignableDates)); // particiant 30 can cover all dates
            participantAvailability.add(new ParticipantAssignment(participants.get(31), assignableSundays)); // particiant 31 can cover every sunday
            participantAvailability.add(new ParticipantAssignment(participants.get(32), assignableSaturdays)); // particiant 32 can cover every saturday
            participantAvailability.add(new ParticipantAssignment(participants.get(33), assignableDates)); // particiant 33 can cover everything
            participantAvailability.add(new ParticipantAssignment(participants.get(34), assignableDates)); // particiant 34 cannot cover everything
        }
    }
}
