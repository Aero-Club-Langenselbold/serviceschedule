package de.ct.manager.schedule.service;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceDate {
    private Date date;
    private Service service;
    private int mandatoryAssignees;

    public ServiceDate(Date date, Service service, int mandatoryAssignees) {
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

        ServiceDate that = (ServiceDate) o;

        return date.equals(that.date) && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return date.hashCode() + service.hashCode();
    }
}
