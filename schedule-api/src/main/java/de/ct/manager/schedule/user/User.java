package de.ct.manager.schedule.user;

import org.hibernate.envers.Audited;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Audited
@Table(name = "auth_user", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends AbstractPersistentEntity {

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole userRole;

    @Column(length = 50, unique = true)
    @NotBlank
    @Email
    private String username;

    @Column(length = 128)
    @NotBlank
    private String password;

    public User() {
        this.userRole = UserRole.ROLE_USER;
    }

    public User(String username, String password) {
        this.userRole = UserRole.ROLE_USER;
        this.username = username;
        this.password = password;
    }
}
