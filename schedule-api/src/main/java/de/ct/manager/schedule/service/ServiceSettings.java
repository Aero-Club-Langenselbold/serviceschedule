package de.ct.manager.schedule.service;

import java.time.LocalTime;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Audited
@Table(name = "service_settings")
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceSettings extends AbstractPersistentEntity {

    @Column(length = 5)
    @NotNull
    private String rangeFrom; // Format: MM-DD

    @Column(length = 5)
    @NotNull
    private String rangeTo; // Format: MM-DD

    @Column
    private int mandatoryAssignees;

    @Column
    private boolean saturdays;

    @Column
    private boolean sundays;

    @Column
    private boolean publicHolidays;

    @Column
    @NotNull
    private LocalTime startTime;

    @Column
    @NotNull
    private LocalTime endTime;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;
}
