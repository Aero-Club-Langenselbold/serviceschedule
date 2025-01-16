package de.ct.manager.schedule.plan;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.ct.manager.schedule.participant.DateAssignment;
import de.ct.manager.schedule.participant.ParticipantAssignment;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@Slf4j
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

    public Plan generatePlan(String planId, List<ServiceDate> serviceDates, List<ParticipantAssignment> availability) {
        Plan plan = planRepository.findById(planId).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }
        if (serviceDates == null || serviceDates.isEmpty()) {
            throw new IllegalArgumentException("No services and dates provided");
        }

        Set<PlanEntry> planEntries = new HashSet<>();
        List<DateAssignment> dateAssignments = generateDateAssignments(serviceDates, availability);

        while (dateAssignments.stream().anyMatch(da -> da.getPossibleAssignees() > 0 || da.getAssignedParticipants().size() < da.getMandatoryAssignees())) {
            try {
                planEntries.add(assignDate(availability, dateAssignments).orElseThrow(() ->
                    new IllegalArgumentException("Plan Entry is empty")));
                log.debug("Assignment made");
                dateAssignments.forEach(da -> log.debug(da.toString()));
            } catch (IllegalArgumentException e) {
                log.error("No assignment possible for date: {}", e.getMessage());
                break;
            }
        }

        plan.getEntries().addAll(planEntries);

        return planRepository.save(plan);
    }

    private Optional<PlanEntry> assignDate(List<ParticipantAssignment> availability, List<DateAssignment> dateAssignments) {
        PlanEntry pe = null;

        // get the smallest number > 0 of possible assignees
        Optional<DateAssignment> daMin = dateAssignments.stream()
            .filter(da -> da.getPossibleAssignees() > 0)
            .min(Comparator.comparingInt(DateAssignment::getPossibleAssignees));

        if (!daMin.isEmpty()) {
            DateAssignment da = daMin.get();
            List<ParticipantAssignment> availableParticipants = getAvailableParticipantsForServiceAndDate(da.getService(), da.getDate(), availability, false);
            pe = generatePlanEntry(da.getService(), da.getDate(), availableParticipants, da);
        } else {
            // if any unassigned date has no more assignable candidates, pick another one
            daMin = dateAssignments.stream()
                .filter(da -> da.getPossibleAssignees() == 0 && da.getAssignedParticipants().size() < da.getMandatoryAssignees())
                .findFirst();
            DateAssignment da = daMin.get();
            List<ParticipantAssignment> availableParticipants = getAvailableParticipantsForServiceAndDate(da.getService(), da.getDate(), availability, true);
            pe = generatePlanEntry(da.getService(), da.getDate(), availableParticipants, da);
        }

        // reset counters for all date assignments and recalculate
        dateAssignments.forEach(da -> {
            da.setPossibleAssignees(0);
            calculateAvailability(availability, da);
        });

        if (pe == null) {
            return Optional.empty();
        }
        return Optional.of(pe);
    }

    private List<DateAssignment> generateDateAssignments(List<ServiceDate> serviceDates, List<ParticipantAssignment> availability) {
        List<DateAssignment> dateAssignments = new ArrayList<>();

        for (ServiceDate serviceDate : serviceDates) {
            DateAssignment da = new DateAssignment(serviceDate.getDate(), serviceDate.getService(), serviceDate.getMandatoryAssignees());

            calculateAvailability(availability, da);

            dateAssignments.add(da);
        }

        // sort list by smallest possible assigneed to assign those first
        return dateAssignments.stream()
            .sorted(Comparator.comparingInt(da -> da.getPossibleAssignees()))
            .toList();
    }

    private void calculateAvailability(List<ParticipantAssignment> availability, DateAssignment da) {
        // count all participants available at that date for that service
        for (ParticipantAssignment pa : availability) {
            if (pa.getDatesAvailable().contains(da.getDate()) && pa.getParticipant().getServices().contains(da.getService())) {
                da.setPossibleAssignees(da.getPossibleAssignees() + 1);
            }
        }

        // set available participants to zero if assignment has already been made
        if (da.getAssignedParticipants().size() == da.getMandatoryAssignees()) {
            da.setPossibleAssignees(0);
        }
    }

    private List<ParticipantAssignment> getAvailableParticipantsForServiceAndDate(Service service, Date date, List<ParticipantAssignment> availability, boolean includeThreshold) {
        List<ParticipantAssignment> availableParticipants = new ArrayList<>();

        for (ParticipantAssignment pa : availability) {
            if (isParticipantAvailableForServiceAndDate(pa, service, date) ||
                (includeThreshold && isParticipantAvailableForServiceAndDateOutsideThreshold(pa, service, date))) {
                availableParticipants.add(pa);
            }
        }

        return availableParticipants;
    }

    private boolean isParticipantAvailableForServiceAndDate(ParticipantAssignment pa, Service service, Date date) {
        return pa.getParticipant().getServices().contains(service) && pa.getDatesAvailable().contains(date);
    }

    private boolean isParticipantAvailableForServiceAndDateOutsideThreshold(ParticipantAssignment pa, Service service, Date date) {
        return pa.getParticipant().getServices().contains(service) && (
            pa.getDatesAvailable().contains(date) ||
            pa.getDatesWithinAssignmentThreshold().contains(date));
    }

    private PlanEntry generatePlanEntry(Service service, Date date, List<ParticipantAssignment> possibleParticipants, DateAssignment dateAssignment) {
        PlanEntry planEntry = new PlanEntry();
        planEntry.setService(service);
        planEntry.setStartTime(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        planEntry.setEndTime(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().plusHours(8)); // Assuming an 8-hour service duration

        // pick the participant with the least possible assignment dates
        // and fewest assignments so far
        ParticipantAssignment selectedParticipant = possibleParticipants.stream()
            .min(Comparator.comparingInt((ParticipantAssignment pa) -> pa.getDatesAssigned().size())
                .thenComparingInt(pa -> pa.getDatesAvailable().size()))
            .orElseThrow(() -> new IllegalArgumentException("No available participants"));

        planEntry.setParticipant(selectedParticipant.getParticipant());

        selectedParticipant.assignDate(date);
        dateAssignment.getAssignedParticipants().add(selectedParticipant.getParticipant());

        return planEntry;
    }
}
