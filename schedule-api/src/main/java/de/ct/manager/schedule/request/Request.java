package de.ct.manager.schedule.request;

import java.util.Date;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "request")
@Data
@EqualsAndHashCode(callSuper = true)
public class Request extends AbstractPersistentEntity {

    @Column(length = 255)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private RequestType type = RequestType.BUG;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private RequestStatus status = RequestStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant author;

    @Column
    private Date createdAt = new Date();
}
