package de.ct.manager.schedule.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByName(UserRole name);
}
