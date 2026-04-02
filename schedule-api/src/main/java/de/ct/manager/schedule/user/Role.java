package de.ct.manager.schedule.user;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "auth_role")
@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends AbstractPersistentEntity {

    @Enumerated(EnumType.STRING)
    @Column(length = 20, unique = true, nullable = false)
    private UserRole name;
}
