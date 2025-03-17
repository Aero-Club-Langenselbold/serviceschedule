package de.ct.manager.schedule.settings;

import java.util.ArrayList;
import java.util.List;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "state")
@Data
@EqualsAndHashCode(callSuper = true)
public class State extends AbstractPersistentEntity {

    @Column(length = 100)
    @NotNull
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    public Country country;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "state_holiday",
        joinColumns = @JoinColumn(name = "state_id"),
        inverseJoinColumns = @JoinColumn(name = "holiday_id")
    )
    public List<Holiday> holidays = new ArrayList<>();

}
