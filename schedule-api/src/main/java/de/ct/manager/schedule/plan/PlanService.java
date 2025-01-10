package de.ct.manager.schedule.plan;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantAssignment;
import de.ct.manager.schedule.service.Service;

@org.springframework.stereotype.Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<Plan> listPlans() {
        return planRepository.findAll();
    }

    public Plan getPlan(String id) {
        return planRepository.findById(id).orElse(null);
    }

    public Plan createOrUpdatePlan(Plan plan) {
        return planRepository.save(plan);
    }

    public void deletePlan(String id) {
        planRepository.deleteById(id);
    }

    public Plan generatePlan(String planId, List<Service> services, List<Date> assignableDates, List<ParticipantAssignment> availability) {
        Plan plan = planRepository.findById(planId).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("No services provided");
        }

        Set<PlanEntry> planEntries = new HashSet<>();

        // Generate plan entries for each service
        for (Service service : services) {
            List<ParticipantAssignment> availableParticipantsForService = getAvailableParticipantsForService(service, availability, plan.getEntries());
            generatePlanEntries(service, assignableDates, availableParticipantsForService, planEntries);
        }

        for (ParticipantAssignment pa : availability) {
            System.out.println(pa.toString());
        }
        List<PlanEntry> entries = planEntries.stream()
            .sorted(Comparator.comparingLong(pa -> pa.getStartTime().toLocalDate().toEpochDay()))
            .collect(Collectors.toList());
        for (PlanEntry pe : entries) {
            System.out.println(pe.toString());
        }

        plan.getEntries().addAll(planEntries);

        return planRepository.save(plan);
    }

    private List<ParticipantAssignment> getAvailableParticipantsForService(Service service, List<ParticipantAssignment> availability, Set<PlanEntry> assignedEntries) {
        List<ParticipantAssignment> availableParticipants = new ArrayList<>();

        for (ParticipantAssignment pa : availability) {
            if (pa.getParticipant().getServices().contains(service)) {
                availableParticipants.add(pa);
            }
        }

        return availableParticipants;
    }

    private void generatePlanEntries(Service service, List<Date> assignableDates,  List<ParticipantAssignment> availableParticipants, Set<PlanEntry> planEntries) {

    /*   int numberOfParticipants = availableParticipants.size();
        int numberOfDates = assignableDates.size();
        int numberOfAssignments = numberOfDates / numberOfParticipants;*/

        // sort participants by availability
        List<ParticipantAssignment> sortedParticipants = sortList(availableParticipants);

        List<Date> assignedDates = new ArrayList<>();
        boolean ignoreThreshold = false;

        while (assignedDates.size() < assignableDates.size()) {
            int currentSize = planEntries.size();
            // assign exactly one date for each participant in one loop

            for (ParticipantAssignment pa : sortedParticipants) {
                List<Date> participantAvailability = pa.getDatesAvailable();

                for (Date date : assignableDates) {
                    if (participantAvailability.contains(date)
                            && !assignedDates.contains(date)) {
                        PlanEntry planEntry = new PlanEntry();
                        planEntry.setService(service);
                        planEntry.setStartTime(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                        planEntry.setEndTime(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().plusHours(8)); // Assuming an 8-hour service duration
                        planEntry.setParticipant(pa.getParticipant());

                        planEntries.add(planEntry);
                        assignedDates.add(date);
                        pa.assignDate(date);
                        break;
                    }
                }
            }

            if (currentSize == planEntries.size() && ignoreThreshold) {
                // nothing has changed during this loop and threshold is ignored, so we need to break out of the loop
                break;
            } else if (currentSize == planEntries.size()) {
                // nothing has changed during this loop, so ignore threshold on next try
                // TODO: Trade some days to see if things work better this way
                // TODO: way to go: see if a participant has an unassigned date within their threshold. if so see if that can be traded with another participant that still has that date available
                // TODO: if you don't understand your way of thinking by what I wrote above tomorrow, I'm very sorry - Past Christian
                ignoreThreshold = true;
            }
        }
    }

    private  List<ParticipantAssignment> sortList(List<ParticipantAssignment> availableParticipants) {
        return availableParticipants.stream()
            .sorted(Comparator.comparingInt(pa -> pa.getDatesAvailable().size()))
            .toList();
    }

    private boolean hasAssignmentWithinThreshold(Date date, Participant participant, Set<PlanEntry> planEntries) {
        List<Date> participantAssignedDates = planEntries.stream()
            .filter(e -> e.getParticipant().equals(participant))
            .map(e -> Date.from(e.getStartTime().atZone(ZoneId.systemDefault()).toInstant()))
            .toList();

        return participantAssignedDates.stream()
            .anyMatch(assignedDate -> Math.abs(date.getTime() - assignedDate.getTime()) < 21L * 24 * 60 * 60 * 1000);
    }
}
