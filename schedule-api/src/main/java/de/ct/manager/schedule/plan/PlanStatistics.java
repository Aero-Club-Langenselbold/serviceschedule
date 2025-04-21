package de.ct.manager.schedule.plan;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.ct.manager.schedule.participant.Participant;
import lombok.Data;

@Data
public class PlanStatistics implements Serializable {
    private Participant participant;
    private Map<String, Long> serviceCount = new HashMap<>();

    public Long getTotalCount() {
        return serviceCount.values().stream().mapToLong(Long::longValue).sum();
    }
}
