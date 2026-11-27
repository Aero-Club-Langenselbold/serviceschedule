package de.ct.manager.schedule.operations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.maintenance.MaintenanceAircraft;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** The checklist of an aircraft (or other equipment) for the start or the end of operations; at most one per aircraft and kind. */
@Entity
@Table(name = "operations_checklist_template", uniqueConstraints = @UniqueConstraint(columnNames = { "aircraft_id", "kind" }))
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationsChecklistTemplate extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceAircraft aircraft;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ChecklistKind kind;

    @ElementCollection
    @CollectionTable(name = "operations_checklist_template_item", joinColumns = @JoinColumn(name = "template_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OrderColumn(name = "position")
    @Column(name = "text", length = 500, nullable = false)
    private List<String> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
