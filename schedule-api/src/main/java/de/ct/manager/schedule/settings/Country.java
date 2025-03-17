package de.ct.manager.schedule.settings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "country")
@Data
@EqualsAndHashCode(callSuper = true)
public class Country extends AbstractPersistentEntity {

    @Column(length = 100)
    @NotNull
    public String name;

    @JsonIgnore
    @OneToMany(mappedBy = "country", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    public List<State> states;
}
