package de.ct.manager.schedule.maintenance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotResponse;
import de.ct.manager.schedule.maintenance.MaintenanceTaskRepository.StepCounts;
import de.ct.manager.schedule.maintenance.MaintenanceTaskRepository.TaskCounts;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * The workshop schedule: periods in which aircraft are planned into the workshop. Periods of different aircraft may overlap.
 * Every period is a workshop visit with its own board of tasks.
 */
@Service
@Slf4j
public class MaintenanceWorkshopService {

    private final MaintenanceWorkshopSlotRepository slotRepository;
    private final MaintenanceAircraftRepository aircraftRepository;
    private final MaintenanceTaskRepository taskRepository;

    public MaintenanceWorkshopService(MaintenanceWorkshopSlotRepository slotRepository, MaintenanceAircraftRepository aircraftRepository,
            MaintenanceTaskRepository taskRepository) {
        this.slotRepository = slotRepository;
        this.aircraftRepository = aircraftRepository;
        this.taskRepository = taskRepository;
    }

    /** All periods, or with aircraftId only those of that aircraft. */
    @Transactional(readOnly = true)
    public List<WorkshopSlotResponse> getSlots(String aircraftId) {
        List<MaintenanceWorkshopSlot> slots = aircraftId == null || aircraftId.isBlank()
                ? slotRepository.findAllByOrderByStartDateAscEndDateAsc()
                : slotRepository.findAllByAircraftIdOrderByStartDateAscEndDateAsc(aircraftId);
        Map<String, Progress> progress = progressBySlot();
        return slots.stream().map(slot -> toResponse(slot, progress.get(slot.getId()))).toList();
    }

    @Transactional
    public WorkshopSlotResponse create(WorkshopSlotRequest request, UserDetailsImpl currentUser) {
        MaintenanceWorkshopSlot slot = new MaintenanceWorkshopSlot();
        apply(slot, request);
        MaintenanceWorkshopSlot saved = slotRepository.save(slot);
        log.info("User {} planned {} into the workshop from {} to {}", currentUser.getUsername(), saved.getAircraft().getRegistration(),
                saved.getStartDate(), saved.getEndDate());
        return toResponse(saved, null);
    }

    @Transactional
    public WorkshopSlotResponse update(String id, WorkshopSlotRequest request, UserDetailsImpl currentUser) {
        MaintenanceWorkshopSlot slot = find(id);
        // the tasks of a visit belong to its aircraft, so a visit with tasks cannot move to another aircraft
        if (!slot.getAircraft().getId().equals(request.aircraftId()) && taskRepository.existsByWorkshopSlot(slot)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The workshop period has tasks of its aircraft");
        }
        apply(slot, request);
        MaintenanceWorkshopSlot saved = slotRepository.saveAndFlush(slot);
        log.info("User {} moved the workshop period of {} to {} – {}", currentUser.getUsername(), saved.getAircraft().getRegistration(),
                saved.getStartDate(), saved.getEndDate());
        return toResponse(saved, progressBySlot().get(saved.getId()));
    }

    /** Removes the period; its tasks are kept on the board without period (SET NULL in the database). */
    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceWorkshopSlot slot = find(id);
        slotRepository.delete(slot);
        log.info("User {} removed the workshop period of {} from {}", currentUser.getUsername(), slot.getAircraft().getRegistration(), slot.getStartDate());
    }

    /** The workshop visit of the aircraft with the given id; null or blank stands for the board without period. */
    MaintenanceWorkshopSlot resolve(MaintenanceAircraft aircraft, String workshopId) {
        if (workshopId == null || workshopId.isBlank()) {
            return null;
        }
        MaintenanceWorkshopSlot slot = slotRepository.findById(workshopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown workshop period"));
        if (!slot.getAircraft().getId().equals(aircraft.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workshop period belongs to another aircraft");
        }
        return slot;
    }

    /** Task and checklist progress per workshop visit; a task without checklist counts as one step. */
    Map<String, Progress> progressBySlot() {
        Map<String, Progress> progress = new HashMap<>();
        for (TaskCounts counts : taskRepository.countBySlot()) {
            progress.computeIfAbsent(counts.getSlotId(), id -> new Progress()).tasks(counts);
        }
        for (StepCounts counts : taskRepository.countChecklistStepsBySlot()) {
            progress.computeIfAbsent(counts.getSlotId(), id -> new Progress()).steps(counts);
        }
        for (StepCounts counts : taskRepository.countTasksWithoutChecklistBySlot()) {
            progress.computeIfAbsent(counts.getSlotId(), id -> new Progress()).steps(counts);
        }
        return progress;
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

    static WorkshopSlotResponse toResponse(MaintenanceWorkshopSlot slot, Progress progress) {
        Progress counts = Objects.requireNonNullElseGet(progress, Progress::new);
        return new WorkshopSlotResponse(slot.getId(), slot.getAircraft().getId(), slot.getStartDate(), slot.getEndDate(), slot.getNote(),
                counts.taskCount, counts.doneCount, counts.signedCount, counts.stepCount, counts.stepDoneCount);
    }

    static final class Progress {
        private long taskCount;
        private long doneCount;
        private long signedCount;
        private long stepCount;
        private long stepDoneCount;

        private void tasks(TaskCounts counts) {
            taskCount += counts.getTotal();
            doneCount += counts.getDone();
            signedCount += counts.getSigned();
        }

        private void steps(StepCounts counts) {
            stepCount += counts.getTotal();
            stepDoneCount += counts.getDone();
        }
    }
}
