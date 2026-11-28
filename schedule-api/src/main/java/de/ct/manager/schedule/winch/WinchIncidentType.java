package de.ct.manager.schedule.winch;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** A kind of incident that can be recorded for a rope, e.g. "Seilriss". */
@Entity
@Table(name = "winch_incident_type")
@Data
@EqualsAndHashCode(callSuper = true)
public class WinchIncidentType extends WinchOption {
}
