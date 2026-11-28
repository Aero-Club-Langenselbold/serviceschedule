package de.ct.manager.schedule.winch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;

import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * The page of the winch book for one day, created with the first entry of that day. It holds the general remark of the
 * day and the winch drivers: everybody who wrote something into it on that day.
 */
@Entity
@Table(name = "winch_day")
@Data
@EqualsAndHashCode(callSuper = true)
public class WinchDay extends AbstractPersistentEntity {

    @Column(name = "day_date", nullable = false, unique = true)
    private LocalDate date;

    @Column(length = 4000)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remark_updated_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User remarkUpdatedBy;

    @Column
    private Instant remarkUpdatedAt;

    // being a driver is no change of the remark, so it does not count for its optimistic locking
    @OptimisticLock(excluded = true)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "winch_day_driver", joinColumns = @JoinColumn(name = "day_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> drivers = new HashSet<>();
}
