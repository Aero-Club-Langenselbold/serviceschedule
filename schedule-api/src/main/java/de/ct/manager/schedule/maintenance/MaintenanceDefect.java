package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Entry of the defect book: a defect or finding someone noticed on an aircraft or other equipment (e.g. a winch).
 * It belongs to the aircraft, not to a workshop visit.
 */
@Entity
@Table(name = "maintenance_defect")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceDefect extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceAircraft aircraft;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DefectPriority priority = DefectPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DefectStatus status = DefectStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User reportedBy;

    @Column(nullable = false)
    private Instant reportedAt = Instant.now();

    /** Who closed the defect (DONE or REJECTED). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "done_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User doneBy;

    @Column
    private Instant doneAt;

    /** Only maintenance administrators sign off a closed defect; a signed off defect is locked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User signedBy;

    @Column
    private Instant signedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "maintenance_defect_assignee",
            joinColumns = @JoinColumn(name = "defect_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> assignees = new HashSet<>();
}
