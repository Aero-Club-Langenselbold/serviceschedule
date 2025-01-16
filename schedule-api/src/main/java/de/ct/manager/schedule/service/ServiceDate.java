package de.ct.manager.schedule.service;

import java.util.Date;

import lombok.Data;

@Data
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
}
