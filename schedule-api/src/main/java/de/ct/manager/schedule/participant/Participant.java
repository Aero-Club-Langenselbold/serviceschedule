package de.ct.manager.schedule.participant;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.ct.manager.schedule.plan.PlanEntry;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
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

    @Column
    private int minAssignments = 0;

    @Column
    private int maxAssignments = 0;

    @Column(length = 36)
    private String registrationKey;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticipantServiceEntity> participantServices = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "participant", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<DateParticipantAvailability> availabilities = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "participant", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PlanEntry> planEntries = new ArrayList<>();

    public String getFullname() {
        return lastname + ", " + firstname;
    }

    public List<Service> getServices() {
        return participantServices.stream()
            .map(ParticipantServiceEntity::getService)
            .toList();
    }

    public void addService(Service service) {
        ParticipantServiceEntity participantServiceEntity = new ParticipantServiceEntity(this, service);
        this.participantServices.add(participantServiceEntity);
    }

    public void setServices(List<Service> services) {
        this.participantServices.clear();
        if (services != null) {
            for (Service service : services) {
                ParticipantServiceEntity participantServiceEntity = new ParticipantServiceEntity(this, service);
                this.participantServices.add(participantServiceEntity);
            }
        }
    }

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

    public boolean equalsForDiff(Participant other) {
        return this.getFirstname().equals(other.getFirstname()) &&
            this.getLastname().equals(other.getLastname()) &&
            this.getEmail().equals(other.getEmail()) &&
            this.getDateOfBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            .equals(other.getDateOfBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) &&
            this.getServices().size() == other.getServices().size() &&
            this.getServices().stream().allMatch(service -> other.getServices().stream().anyMatch(otherService -> service.equals(otherService)));
    }
}
