package de.ct.manager.schedule.participant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.data.util.Pair;

import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ParticipantAssignment {

    private static final long ASSIGNMENT_THRESHOLD = 8L * 24 * 60 * 60 * 1000; // 8 days
    private static final long NO_WAY_THRESHOLD = 3L * 24 * 60 * 60 * 1000; // 3 days

    private Participant participant;
    private List<Pair<Date, Service>> datesRequested = new CopyOnWriteArrayList<>();
    private List<Date> datesAvailable = new CopyOnWriteArrayList<>();
    private List<Date> datesAssigned = new ArrayList<>();
    private List<Date> datesWithinAssignmentThreshold = new ArrayList<>();
    private List<Service> availableServices;
    private int nextServiceIndex = 0;

    public ParticipantAssignment(Participant participant, List<Date> datesAvailable) {
        this(participant, datesAvailable, null);
    }

    public ParticipantAssignment(Participant participant, List<Date> datesAvailable, List<Pair<Date, Service>>  datesRequested) {
        this.participant = participant;
        this.datesAvailable = new CopyOnWriteArrayList<>(datesAvailable);
        if (datesRequested != null) {
            this.datesRequested = new CopyOnWriteArrayList<>(datesRequested);
            this.datesAvailable.removeAll(datesRequested.stream()
                .map(Pair::getFirst)
                .collect(Collectors.toList()));
        }
        this.availableServices = participant.getParticipantServices().stream()
            .map(ParticipantServiceEntity::getService)
            .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
    }

    public boolean assignDate(Date date) {
        String dateFormat = "yyyy-MM-dd";
        String isoDate = new SimpleDateFormat(dateFormat).format(date);
        boolean isDateAvailable = datesAvailable.stream()
            .map(d -> new SimpleDateFormat(dateFormat).format(d))
            .anyMatch(d -> d.equals(isoDate));
        boolean isDateRequested = datesRequested.stream()
            .map(d -> new SimpleDateFormat(dateFormat).format(d.getFirst()))
            .anyMatch(d -> d.equals(isoDate));
        boolean isDateWithinThreshold = datesWithinAssignmentThreshold.stream()
            .map(d -> new SimpleDateFormat(dateFormat).format(d))
            .anyMatch(d -> d.equals(isoDate));

        if (!isDateAvailable && !isDateRequested && !isDateWithinThreshold) {
            return false;
        }

        datesAssigned.add(date);
        datesRequested.remove(datesRequested.stream()
            .filter(d -> d.getFirst().equals(date))
            .findFirst()
            .orElse(null));
        datesAvailable.remove(date);
        datesWithinAssignmentThreshold.remove(date);
        moveDatesWithinThreshold(date);
        determineNextService();

        return true;
    }

    public boolean canAssignService(ServiceDate serviceDate) {
        Service service = serviceDate.getService();
        Date date = serviceDate.getDate();
        if (availableServices.contains(service)) {
            // Find the corresponding ParticipantServiceEntity
            ParticipantServiceEntity participantServiceEntity = participant.getParticipantServices().stream()
                .filter(ps -> ps.getService().equals(service))
                .findFirst()
                .orElse(null);

            // If autoAssign is false, check if the service was specifically requested
            if (participantServiceEntity != null) {
                if (datesRequested.stream().anyMatch(pair -> pair.getFirst().equals(date) && pair.getSecond().equals(service))) {
                    return true;
                } else if (!participantServiceEntity.isAutoAssign()){
                    return false;
                }
            }
        }

        if (availableServices.isEmpty() || (participant.getMaxAssignments() > 0 && datesAssigned.size() >= participant.getMaxAssignments())) {
            return false; // only return true if the participant has not reached their max assignments (0 max assignments means unlimited)
        }
        else if (availableServices.contains(service) && datesAssigned.isEmpty()) {
            return true;
        } else if (datesWithinAssignmentThreshold.contains(date)) {
            return false;
        } else {
            return availableServices.get(this.nextServiceIndex).equals(service);
        }
    }

    /**
     * when all dates for a specific service are assigned, that service need to be removed for every participant
     * @param service
     */
    public void removeFromAvailableServices(Service service) {
        if (availableServices.isEmpty()) {
            return;
        }
        if (availableServices.contains(service)) {
            availableServices.remove(service);
            determineNextService();
        }
    }

    private void determineNextService() {
        if (availableServices.isEmpty()) {
            return;
        } else  if (nextServiceIndex + 1 >= availableServices.size()) {
            nextServiceIndex = 0;
        } else {
            nextServiceIndex++;
        }
    }

    private void moveDatesWithinThreshold(Date assignedDate) {
        // avoid participants being assigned to dates that are too close to the assigned date
        List<Date> nextDayThreshold = datesAvailable.stream()
            .filter(date -> Math.abs(date.getTime() - assignedDate.getTime()) < NO_WAY_THRESHOLD)
            .toList();
        datesAvailable.removeAll(nextDayThreshold);
        datesWithinAssignmentThreshold.removeAll(nextDayThreshold);

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
