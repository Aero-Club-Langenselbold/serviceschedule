package de.ct.manager.schedule.participant;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantServiceId implements Serializable {
    private String participantId;
    private String serviceId;
}
