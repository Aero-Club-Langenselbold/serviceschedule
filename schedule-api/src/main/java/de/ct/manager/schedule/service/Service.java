package de.ct.manager.schedule.service;

import java.util.Set;

import org.hibernate.envers.Audited;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Audited
@Table(name = "service")
@Data
@EqualsAndHashCode(callSuper = true)
public class Service extends AbstractPersistentEntity {

    @Column(length = 150)
    @NotBlank
    @NotNull
    private String servicename;

    @ManyToMany(mappedBy = "services", fetch = FetchType.LAZY)
    private Set<Participant> participants;
}
