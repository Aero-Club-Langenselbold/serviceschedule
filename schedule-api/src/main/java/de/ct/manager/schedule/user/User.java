package de.ct.manager.schedule.user;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "auth_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @NotAudited
    private Set<Role> roles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "auth_user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @NotAudited
    private Set<UserPermission> permissions = new HashSet<>();

    @Column(length = 50, unique = true)
    @NotBlank
    @Email
    private String username;

    @Column(length = 128)
    @NotBlank
    private String password;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
