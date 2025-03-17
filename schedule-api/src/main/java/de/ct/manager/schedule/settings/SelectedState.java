package de.ct.manager.schedule.settings;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "selected_state")
@Data
@EqualsAndHashCode(callSuper = true)
public class SelectedState extends AbstractPersistentEntity {

    @OneToOne(fetch = FetchType.LAZY)
    public Country country;

    @OneToOne(fetch = FetchType.LAZY)
    public State state;
}
