package de.ct.manager.schedule.operations;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.maintenance.MaintenanceDefect;
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

/** One step of a day's checklist: checked as OK or NOT_OK by a member, optionally with a comment and a reported defect. */
@Entity
@Table(name = "operations_checklist_item", uniqueConstraints = @UniqueConstraint(columnNames = { "checklist_id", "position" }))
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationsChecklistItem extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OperationsChecklist checklist;

    @Column(nullable = false)
    private int position;

    @Column(length = 500, nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ChecklistItemResult result;

    @Column(length = 1000)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User checkedBy;

    @Column
    private Instant checkedAt;

    /** The defect reported to the defect book from this item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceDefect defect;
}
