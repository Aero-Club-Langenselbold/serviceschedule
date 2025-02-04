package de.ct.manager.schedule.plan;

import java.util.ArrayList;
import java.util.List;

import de.ct.manager.schedule.participant.ParticipantAssignment;
import de.ct.manager.schedule.service.ServiceDate;
import lombok.Data;

@Data
public class PlanDetails {

    private List<ServiceDate> serviceDates = new ArrayList<>();
    private List<ParticipantAssignment> availability = new ArrayList<>();
}