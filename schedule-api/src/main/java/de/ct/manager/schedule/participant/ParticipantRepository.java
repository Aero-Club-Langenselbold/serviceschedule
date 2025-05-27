package de.ct.manager.schedule.participant;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParticipantRepository extends JpaRepository<Participant, String> {

    Optional<Participant> findByEmail(String email);

    @Query("SELECT p FROM Participant p WHERE "
            + "LOWER(p.firstname) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.lastname) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "p in (SELECT ps.participant FROM ParticipantServiceEntity ps WHERE ps.service in (SELECT s FROM Service s WHERE LOWER(s.servicename) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Participant> findAll(Pageable pageable, String search);
}
