package de.ct.manager.schedule.participant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.ct.manager.schedule.service.ServiceDate;
import lombok.Data;

@Data
public class DateAssignment {
    private ServiceDate serviceDate;
    private int possibleAssignees;
    private List<Participant> assignedParticipants = new ArrayList<>();

    public DateAssignment(ServiceDate serviceDate) {
        if (serviceDate == null) {
            throw new IllegalArgumentException("Date and service must not be null");
        }
        if (serviceDate.getMandatoryAssignees() < 1) {
            throw new IllegalArgumentException("Mandatory assignees must not be less than 1");
        }

        this.serviceDate = serviceDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DateAssignment that = (DateAssignment) o;

        return serviceDate.equals(that.serviceDate);
    }

    @Override
    public int hashCode() {
        return serviceDate.hashCode();
    }

    @Override
    public String toString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        return "DateAssignment{" +
            "date=" + df.format(serviceDate.getDate()) +
            ", service=" + serviceDate.getService().getServicename() +
            ", mandatoryAssignees=" + serviceDate.getMandatoryAssignees() +
            ", possibleAssignees=" + possibleAssignees +
            ", assignedParticipants=" + (assignedParticipants == null ? "0" : assignedParticipants.size()) +
            '}';
    }
}
