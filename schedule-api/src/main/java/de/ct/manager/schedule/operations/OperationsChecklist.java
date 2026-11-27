package de.ct.manager.schedule.operations;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.maintenance.MaintenanceAircraft;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * The start or end of operations checklist of one aircraft on one day, filled in by members. Its items are copied from
 * the template when it is started, so it stays as it was when the template changes. The member who started it is
 * recorded as initial recorder and never changes.
 */
@Entity
@Table(name = "operations_checklist", uniqueConstraints = @UniqueConstraint(columnNames = { "aircraft_id", "kind", "checklist_date" }))
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationsChecklist extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceAircraft aircraft;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ChecklistKind kind;

    /** The template it was started from; null once that template is deleted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OperationsChecklistTemplate template;

    @Column(name = "checklist_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ChecklistStatus status = ChecklistStatus.IN_PROGRESS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User submittedBy;

    @Column
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User approvedBy;

    @Column
    private Instant approvedAt;
}
