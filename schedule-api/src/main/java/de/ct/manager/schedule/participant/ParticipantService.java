package de.ct.manager.schedule.participant;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.ct.manager.schedule.service.ServiceRepository;

@Service
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final ServiceRepository serviceRepository;

    public ParticipantService(ParticipantRepository participantRepository, ServiceRepository serviceRepository) {
        this.participantRepository = participantRepository;
        this.serviceRepository = serviceRepository;
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
        if (participant.getServices() != null) {
            participant.setServices(participant.getServices().stream().map(service -> {
                if (service.getId() != null) {
                    return serviceRepository.findById(service.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + service.getId()));
                }
                return service;
            }).toList());
        }
        return participantRepository.save(participant);
    }

    public void deleteParticipant(String id) {
        participantRepository.deleteById(id);
    }
}
