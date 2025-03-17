package de.ct.manager.schedule.settings;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "holiday")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Holiday extends AbstractPersistentEntity {

    @Column(length = 100)
    @NotNull
    public String name;

    @Column(length = 5)
    public String date;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    public Formula formula;
}
