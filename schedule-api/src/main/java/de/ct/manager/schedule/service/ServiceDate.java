package de.ct.manager.schedule.service;

import java.time.LocalTime;
import java.util.Date;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.plan.Plan;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Audited
@Table(name = "service_date")
@Data
@NoArgsConstructor
public class ServiceDate extends AbstractPersistentEntity {
    @Column
    @NotNull
    private Date date;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id")
    private Service service;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column
    @NotNull
    private int mandatoryAssignees = 1;

    @Column
    @NotNull
    private LocalTime startTime;

    @Column
    @NotNull
    private LocalTime endTime;

    public ServiceDate(Date date, Service service, int mandatoryAssignees, LocalTime startTime, LocalTime endTime) {
        if (date == null || service == null) {
            throw new IllegalArgumentException("Date and service must not be null");
        }
        if (mandatoryAssignees < 1) {
            throw new IllegalArgumentException("Mandatory assignees must not be less than 1");
        }

        this.date = date;
        this.service = service;
        this.mandatoryAssignees = mandatoryAssignees;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceDate that = (ServiceDate) o;

        return date.equals(that.date) && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return date.hashCode() + service.hashCode();
    }
}
