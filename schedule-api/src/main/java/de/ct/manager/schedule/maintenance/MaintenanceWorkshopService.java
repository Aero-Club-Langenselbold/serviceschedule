package de.ct.manager.schedule.maintenance;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotResponse;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

/** The workshop schedule: periods in which aircraft are planned into the workshop. Periods of different aircraft may overlap. */
@Service
@Slf4j
public class MaintenanceWorkshopService {

    private final MaintenanceWorkshopSlotRepository slotRepository;
    private final MaintenanceAircraftRepository aircraftRepository;

    public MaintenanceWorkshopService(MaintenanceWorkshopSlotRepository slotRepository, MaintenanceAircraftRepository aircraftRepository) {
        this.slotRepository = slotRepository;
        this.aircraftRepository = aircraftRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkshopSlotResponse> getSlots() {
        return slotRepository.findAllByOrderByStartDateAscEndDateAsc().stream().map(MaintenanceWorkshopService::toResponse).toList();
    }

    @Transactional
    public WorkshopSlotResponse create(WorkshopSlotRequest request, UserDetailsImpl currentUser) {
        MaintenanceWorkshopSlot slot = new MaintenanceWorkshopSlot();
        apply(slot, request);
        MaintenanceWorkshopSlot saved = slotRepository.save(slot);
        log.info("User {} planned {} into the workshop from {} to {}", currentUser.getUsername(), saved.getAircraft().getRegistration(),
                saved.getStartDate(), saved.getEndDate());
        return toResponse(saved);
    }

    @Transactional
    public WorkshopSlotResponse update(String id, WorkshopSlotRequest request, UserDetailsImpl currentUser) {
        MaintenanceWorkshopSlot slot = find(id);
        apply(slot, request);
        MaintenanceWorkshopSlot saved = slotRepository.saveAndFlush(slot);
        log.info("User {} moved the workshop period of {} to {} – {}", currentUser.getUsername(), saved.getAircraft().getRegistration(),
                saved.getStartDate(), saved.getEndDate());
        return toResponse(saved);
    }

    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceWorkshopSlot slot = find(id);
        slotRepository.delete(slot);
        log.info("User {} removed the workshop period of {} from {}", currentUser.getUsername(), slot.getAircraft().getRegistration(), slot.getStartDate());
    }

    private MaintenanceWorkshopSlot find(String id) {
        return slotRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Workshop period"));
    }

    private void apply(MaintenanceWorkshopSlot slot, WorkshopSlotRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workshop period ends before it starts");
        }
        MaintenanceAircraft aircraft = aircraftRepository.findById(request.aircraftId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown aircraft"));
        // the same aircraft cannot be in the workshop twice at the same time
        if (slotRepository.existsOverlap(aircraft, request.startDate(), request.endDate(), slot.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The aircraft is already planned in this period");
        }
        slot.setAircraft(aircraft);
        slot.setStartDate(request.startDate());
        slot.setEndDate(request.endDate());
        slot.setNote(request.note() == null || request.note().isBlank() ? null : request.note().strip());
    }

    static WorkshopSlotResponse toResponse(MaintenanceWorkshopSlot slot) {
        return new WorkshopSlotResponse(slot.getId(), slot.getAircraft().getId(), slot.getStartDate(), slot.getEndDate(), slot.getNote());
    }
}
