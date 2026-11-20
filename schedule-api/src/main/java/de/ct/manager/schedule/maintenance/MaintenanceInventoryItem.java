package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** A consumable or spare part in the workshop stock. */
@Entity
@Table(name = "maintenance_inventory_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceInventoryItem extends AbstractPersistentEntity {

    @Column(length = 255, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private InventoryKind kind = InventoryKind.CONSUMABLE;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private InventoryCounting counting = InventoryCounting.COUNT;

    @Column(nullable = false)
    private int quantity;

    @Column
    private Integer minQuantity;

    @Column(length = 30)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Aircraft types a spare part fits, empty means every aircraft. Always empty for consumables. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "maintenance_inventory_item_aircraft_type", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "aircraft_type", length = 100, nullable = false)
    private Set<String> aircraftTypes = new HashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public InventoryStockStatus getStatus() {
        if (quantity == 0) {
            return InventoryStockStatus.EMPTY;
        }
        if (counting == InventoryCounting.LEVEL) {
            return quantity == InventoryCounting.LEVEL_LOW ? InventoryStockStatus.LOW : InventoryStockStatus.OK;
        }
        return minQuantity != null && quantity <= minQuantity ? InventoryStockStatus.LOW : InventoryStockStatus.OK;
    }
}
