package de.ct.manager.schedule.maintenance;

import java.time.Instant;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** An aircraft (or any other object) that is maintained in the workshop; its workshop periods are {@link MaintenanceWorkshopSlot}s. */
@Entity
@Table(name = "maintenance_aircraft")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceAircraft extends AbstractPersistentEntity {

    @Column(name = "aircraft_type", length = 100, nullable = false)
    private String type;

    @Column(length = 20, nullable = false, unique = true)
    private String registration;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
