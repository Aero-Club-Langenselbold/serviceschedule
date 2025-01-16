package de.ct.manager.schedule.participant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.ct.manager.schedule.service.Service;
import lombok.Data;

@Data
public class DateAssignment {
    private Date date;
    private Service service;
    private int possibleAssignees;
    private int mandatoryAssignees;
    private List<Participant> assignedParticipants = new ArrayList<>();

    public DateAssignment(Date date, Service service, int mandatoryAssignees) {
        if (date == null || service == null) {
            throw new IllegalArgumentException("Date and service must not be null");
        }
        if (mandatoryAssignees < 1) {
            throw new IllegalArgumentException("Mandatory assignees must not be less than 1");
        }

        this.date = date;
        this.service = service;
        this.mandatoryAssignees = mandatoryAssignees;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DateAssignment that = (DateAssignment) o;

        return date.equals(that.date) && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return date.hashCode() + service.hashCode();
    }

    @Override
    public String toString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        return "DateAssignment{" +
            "date=" + df.format(date) +
            ", service=" + service.getServicename() +
            ", possibleAssignees=" + possibleAssignees +
            ", assignedParticipants=" + (assignedParticipants == null ? "0" : assignedParticipants.size()) +
            '}';
    }
}
