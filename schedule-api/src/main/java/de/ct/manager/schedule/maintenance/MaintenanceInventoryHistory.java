package de.ct.manager.schedule.maintenance;

import java.time.Instant;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** What happened to an inventory item; {@code quantity} is the stock after the action. */
@Entity
@Table(name = "maintenance_inventory_history")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceInventoryHistory extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceInventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private InventoryHistoryAction action;

    @Column
    private Integer quantity;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
