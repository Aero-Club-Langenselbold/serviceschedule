package de.ct.manager.schedule.winch;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** A rope of the winch. */
@Entity
@Table(name = "winch_rope")
@Data
@EqualsAndHashCode(callSuper = true)
public class WinchRope extends WinchOption {
}
