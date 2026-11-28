package de.ct.manager.schedule.winch;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Something maintenance administrators define for the winch book: a rope or a kind of incident. */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class WinchOption extends AbstractPersistentEntity {

    @Column(length = 100, nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int position;

    /** Inactive options stay in the recorded entries but can no longer be chosen. */
    @Column(nullable = false)
    private boolean active = true;
}
