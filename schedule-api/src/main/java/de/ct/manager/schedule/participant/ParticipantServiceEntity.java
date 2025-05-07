package de.ct.manager.schedule.participant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.service.Service;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "participant_service")
@NoArgsConstructor
public class ParticipantServiceEntity {
    @EmbeddedId
    private ParticipantServiceId id = new ParticipantServiceId();

    @JsonIgnore
    @ManyToOne
    @MapsId("participantId")
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private Service service;

    @Column
    private boolean autoAssign;

    public ParticipantServiceEntity(Participant participant, Service service) {
        this.participant = participant;
        this.service = service;
        this.id = new ParticipantServiceId(participant.getId(), service.getId());
        this.autoAssign = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantServiceEntity that = (ParticipantServiceEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
