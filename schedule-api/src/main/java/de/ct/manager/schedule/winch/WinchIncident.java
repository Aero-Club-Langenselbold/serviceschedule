package de.ct.manager.schedule.winch;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** An incident of a rope (e.g. a rope break), recorded by the winch driver on the day it happened. */
@Entity
@Table(name = "winch_incident")
@Data
@EqualsAndHashCode(callSuper = true)
public class WinchIncident extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WinchDay day;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rope_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WinchRope rope;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WinchIncidentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User driver;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 1000)
    private String comment;
}
