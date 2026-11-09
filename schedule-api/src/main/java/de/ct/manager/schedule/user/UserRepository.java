package de.ct.manager.schedule.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE "
            + "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "u.username IN (SELECT p.email FROM Participant p WHERE "
            + "LOWER(p.firstname) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.lastname) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findAll(Pageable pageable, String search);

}
