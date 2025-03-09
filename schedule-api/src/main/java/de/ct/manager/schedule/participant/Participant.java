package de.ct.manager.schedule.participant;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Audited
@Table(name = "participant")
@Data
@EqualsAndHashCode(callSuper = true)
public class Participant extends AbstractPersistentEntity {

    @Column(length = 50)
    @NotBlank
    @NotNull
    private String firstname;

    @Column(length = 50)
    @NotBlank
    @NotNull
    private String lastname;

    @Column(length = 150)
    @NotBlank
    @NotNull
    private String email;

    @Column
    @NotNull
    private Date dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Status status = Status.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "participant_service",
        joinColumns = @JoinColumn(name = "participant_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<Service> services = new ArrayList<>();

    @JsonIgnore
    public int getCurrentAge() {
        return Period.between(dateOfBirth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()).getYears();
    }

    @JsonIgnore
    public int getAgeAtDate(LocalDate date) {
        return Period.between(dateOfBirth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
            date).getYears();
    }

    @Override
    public String toString() {
        return "Participant{" +
            "id=" + getId() +
            ", firstname='" + firstname + '\'' +
            ", lastname='" + lastname + '\'' +
            ", email='" + email + '\'' +
            ", dateOfBirth=" + dateOfBirth +
            ", status=" + status +
            '}';
    }
}
