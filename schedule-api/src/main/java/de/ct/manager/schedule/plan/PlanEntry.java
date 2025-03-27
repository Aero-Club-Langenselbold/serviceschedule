package de.ct.manager.schedule.plan;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.service.Service;
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
@Table(name = "plan_entry")
@Data
@EqualsAndHashCode(callSuper = true)
public class PlanEntry extends AbstractPersistentEntity {

    @Column
    @NotNull
    private LocalDateTime startTime;

    @Column
    @NotNull
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    public LocalDate getDate() {
        return startTime.toLocalDate();
    }

    @Override
    public String toString() {
        return "PlanEntry{" +
            "\r\n\tstartTime=" + startTime +
            "\r\n\tendTime=" + endTime +
            "\r\n\tservice=" + service.getServicename() +
            "\r\n\tparticipant=" + participant.getEmail() +
            "\r\n}";
    }
}
