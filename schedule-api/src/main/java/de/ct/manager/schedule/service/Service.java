package de.ct.manager.schedule.service;

import java.util.Set;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Audited
@Table(name = "service")
@Data
public class Service extends AbstractPersistentEntity {

    @Column(length = 150)
    @NotBlank
    @NotNull
    private String servicename;

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<ServiceSettings> settings;

    @JsonIgnore
    @ManyToMany(mappedBy = "services", fetch = FetchType.LAZY)
    private Set<Participant> participants;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Service service = (Service) o;

        return servicename.equals(service.servicename) && getId().equals(service.getId());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + servicename.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Service{" +
            "id=" + getId() +
            ", servicename='" + servicename + '\'' +
            '}';
    }
}
