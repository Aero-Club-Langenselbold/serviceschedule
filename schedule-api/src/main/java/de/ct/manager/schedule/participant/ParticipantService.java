package de.ct.manager.schedule.participant;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ParticipantService {
    private final ParticipantRepository participantRepository;

    public ParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    public List<Participant> listParticipants() {
        return participantRepository.findAll();
    }

    public Page<Participant> listParticipants(Pageable pageable) {
        return participantRepository.findAll(pageable);
    }

    public Participant getParticipant(String id) {
        return participantRepository.findById(id).orElse(null);
    }

    public Participant getParticipantByEmail(String email) {
        return participantRepository.findByEmail(email).orElse(null);
    }

    public Participant createOrUpdateParticipant(Participant participant) {
        return participantRepository.save(participant);
    }

    public void deleteParticipant(String id) {
        participantRepository.deleteById(id);
    }
}
