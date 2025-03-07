package de.ct.manager.schedule.participant;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, String> {

    Optional<Participant> findByEmail(String email);
}
