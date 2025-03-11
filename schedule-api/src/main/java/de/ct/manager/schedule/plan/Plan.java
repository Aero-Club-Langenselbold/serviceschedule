package de.ct.manager.schedule.plan;

import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CascadeType;
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

    @Column
    private boolean published;

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<PlanEntry> entries = new HashSet<>();

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<ServiceDate> serviceDates = new HashSet<>();

    @JsonIgnore
    public List<Service> getAllServices() {
        return entries.stream()
            .map(PlanEntry::getService)
            .distinct()
            .toList();
    }

    @JsonIgnore
    public List<Date> getAllDates() {
        return entries.stream()
            .map(entry -> entry.getStartTime().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant())
            .map(Date::from)
            .distinct()
            .sorted()
            .toList();
    }
}
