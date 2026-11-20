package de.ct.manager.schedule.maintenance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Blueprint for tasks; administrators create the tasks of an aircraft from the templates that apply to its type. */
@Entity
@Table(name = "maintenance_template")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceTemplate extends AbstractPersistentEntity {

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20, nullable = false)
    private SkillLevel level = SkillLevel.BEGINNER;

    @Column
    private Integer durationMinutes;

    /** Aircraft types the template applies to, empty means every aircraft. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "maintenance_template_aircraft_type", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "aircraft_type", length = 100, nullable = false)
    private Set<String> aircraftTypes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "maintenance_template_checklist_item", joinColumns = @JoinColumn(name = "template_id"))
    @OrderColumn(name = "position")
    @Column(name = "text", length = 500, nullable = false)
    private List<String> checklist = new ArrayList<>();

    public boolean appliesTo(String aircraftType) {
        return aircraftTypes.isEmpty() || aircraftTypes.stream().anyMatch(type -> type.equalsIgnoreCase(aircraftType));
    }
}
