package de.ct.manager.schedule.participant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        return participantRepository.findAll(Sort.by("lastname").ascending().and(Sort.by("firstname").ascending()));
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
        if (participant.getId().isEmpty()) {
            participant.setId(UUID.randomUUID().toString());
        }
        // clear old participant services list
        Participant oldParticipant = participantRepository.findById(participant.getId()).orElse(null);
        if (oldParticipant != null && oldParticipant.getParticipantServices() != null) {
            oldParticipant.getParticipantServices().clear();
            participantRepository.save(oldParticipant);
        }
        if (participant.getParticipantServices() != null) {
            participant.setParticipantServices(participant.getParticipantServices().stream().map(ps -> {
                if (ps.getService().getId() != null) {
                    de.ct.manager.schedule.service.Service service = serviceRepository.findById(ps.getService().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + ps.getService().getId()));
                    ps.setParticipant(participant);
                    ps.setService(service);
                }
                return ps;
            }).toList());
        }
        if (participant.getRegistrationKey() == null || participant.getRegistrationKey().isEmpty()) {
            participant.setRegistrationKey(UUID.randomUUID().toString());
        }
        return participantRepository.save(participant);
    }

    public void deleteParticipant(String id) {
        participantRepository.deleteById(id);
    }

    public ParticipantDiff importParticipants(MultipartFile file) {
        ParticipantDiff participantDiff = new ParticipantDiff();
        List<String> csvEmails = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.ISO_8859_1))) {
            reader.lines().skip(1).forEach(line -> {
                String[] columns = line.replace("\"", "").split(";");
                if (columns.length < 5) {
                    throw new IllegalArgumentException("Invalid CSV format");
                }
                String[] nameParts = columns[1].split(", ");
                String lastName = nameParts[0];
                String firstName = nameParts.length > 1 ? nameParts[1] : "";
                LocalDate dateOfBirth = LocalDate.parse(columns[2], DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                String email = columns[3];
                csvEmails.add(email);

                List<de.ct.manager.schedule.service.Service> serviceList = new ArrayList<>();
                if (columns.length > 5) {
                    String[] services = columns[5].split(", ");
                    serviceList = serviceRepository.findAll().stream()
                        .filter(service -> List.of(services).contains(service.getServicename()) || service.isAddAtImport())
                        .toList();
                } else {
                    serviceList = serviceRepository.findAll().stream()
                        .filter(service -> service.isAddAtImport())
                        .toList();
                }

                Participant participant = new Participant();
                participant.setFirstname(firstName);
                participant.setLastname(lastName);
                participant.setDateOfBirth(Date.from(dateOfBirth.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                participant.setEmail(email);
                participant.setRegistrationKey(UUID.randomUUID().toString());
                participant.setServices(serviceList);

                participantRepository.findByEmail(email).ifPresentOrElse(existingParticipant -> {
                    if (!existingParticipant.equalsForDiff(participant)) {
                        participantDiff.getUpdatedParticipants().add(participant);
                    } else {
                        participantDiff.getUnchangedParticipants().add(participant);
                    }
                }, () -> participantDiff.getNewParticipants().add(participant));
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }

        List<Participant> allParticipants = participantRepository.findAll();
        allParticipants.stream()
            .filter(participant -> !csvEmails.contains(participant.getEmail()))
            .forEach(participantDiff.getMissingParticipants()::add);

        return participantDiff;
    }
}
