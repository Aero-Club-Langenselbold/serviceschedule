package de.ct.manager.schedule.participant;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.ct.manager.schedule.service.Service;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ParticipantAssignment {

    private static final long ASSIGNMENT_THRESHOLD = 21L * 24 * 60 * 60 * 1000; // 21 days

    private Participant participant;
    private List<Date> datesRequested = new CopyOnWriteArrayList<>();
    private List<Date> datesAvailable = new CopyOnWriteArrayList<>();
    private List<Date> datesAssigned = new ArrayList<>();
    private List<Date> datesWithinAssignmentThreshold = new ArrayList<>();
    private List<Service> availableServices;
    private int nextServiceIndex = 0;

    public ParticipantAssignment(Participant participant, List<Date> datesAvailable) {
        this(participant, datesAvailable, null);
    }

    public ParticipantAssignment(Participant participant, List<Date> datesAvailable, List<Date> datesRequested) {
        this.participant = participant;
        this.datesAvailable = new CopyOnWriteArrayList<>(datesAvailable);
        if (datesRequested != null) {
            this.datesRequested = new CopyOnWriteArrayList<>(datesRequested);
            this.datesAvailable.removeAll(datesRequested);
        }
        this.availableServices = new ArrayList<>(participant.getServices());
    }

    public boolean assignDate(Date date) {
        if (!datesAvailable.contains(date) && !datesRequested.contains(date)) {
            return false;
        }

        datesAssigned.add(date);
        datesRequested.remove(date);
        datesAvailable.remove(date);
        moveDatesWithinThreshold(date);
        determineNextService();

        return true;
    }

    public boolean canAssignService(Service service) {
        if (participant.getMaxAssignments() > 0 && datesAssigned.size() >= participant.getMaxAssignments()) {
            return false; // only return true if the participant has not reached their max assignments (0 max assignments means unlimited)
        }
        else if (availableServices.contains(service) && datesAssigned.isEmpty()) {
            return true;
        } else {
            return availableServices.get(this.nextServiceIndex).equals(service);
        }
    }

    private void determineNextService() {
        if (nextServiceIndex + 1 >= availableServices.size()) {
            nextServiceIndex = 0;
        } else {
            nextServiceIndex++;
        }
    }

    private void moveDatesWithinThreshold(Date assignedDate) {
        List<Date> datesWithinThreshold = datesAvailable.stream()
            .filter(date -> Math.abs(date.getTime() - assignedDate.getTime()) < ASSIGNMENT_THRESHOLD)
            .toList();
        datesWithinAssignmentThreshold.addAll(datesWithinThreshold);
        datesAvailable.removeAll(datesWithinThreshold);
    }

    @Override
    public String toString() {
        return "ParticipantAssignment{" +
            "\r\n\tparticipant=" + participant.getEmail() +
            "\r\n\tdatesAvailable=" + datesAvailable +
            "\r\n\tdatesAssigned=" + datesAssigned +
            "\r\n\tdatesWithinAssignmentThreshold=" + datesWithinAssignmentThreshold +
            "\r\n}";
    }
}
