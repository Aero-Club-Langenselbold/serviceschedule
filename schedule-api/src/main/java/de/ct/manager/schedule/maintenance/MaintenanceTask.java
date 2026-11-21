package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** A kanban card: one piece of work on an aircraft. */
@Entity
@Table(name = "maintenance_task")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceTask extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceAircraft aircraft;

    /** Workshop visit whose board shows the task; null for tasks outside a workshop period. Same aircraft as the task. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_slot_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceWorkshopSlot workshopSlot;

    /** Template the task was created from, null for tasks created by hand. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceTemplate template;

    /** Defect or finding the task resolves. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_id", unique = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceDefect defect;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20, nullable = false)
    private SkillLevel level = SkillLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TaskStatus status = TaskStatus.OPEN;

    @Column
    private Integer durationMinutes;

    @Column
    private LocalDate deadline;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "done_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User doneBy;

    @Column
    private Instant doneAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User signedBy;

    @Column
    private Instant signedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MaintenanceTaskChecklistItem> checklist = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MaintenanceTaskNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MaintenanceTaskHistory> history = new ArrayList<>();

    /** Signed off tasks are locked until an administrator withdraws the sign-off. */
    public boolean isSigned() {
        return signedAt != null;
    }

    public void log(TaskHistoryAction action, User actor) {
        MaintenanceTaskHistory entry = new MaintenanceTaskHistory();
        entry.setTask(this);
        entry.setAction(action);
        entry.setActor(actor);
        history.add(0, entry);
    }
}
