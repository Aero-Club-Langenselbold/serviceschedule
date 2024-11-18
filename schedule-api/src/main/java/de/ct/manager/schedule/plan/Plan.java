package de.ct.manager.schedule.plan;

import java.util.Date;
import java.util.Set;

import org.hibernate.envers.Audited;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Audited
@Table(name = "plan")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "entries")
public class Plan extends AbstractPersistentEntity {

    @Column(length = 50)
    @NotBlank
    @NotNull
    private String name;

    @Column(length = 255)
    private String description;

    @Column
    @NotNull
    private Date dateFrom;

    @Column
    @NotNull
    private Date dateUntil;

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<PlanEntry> entries;
}
