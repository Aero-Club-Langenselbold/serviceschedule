package de.ct.manager.schedule.maintenance;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.AircraftRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.AircraftResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.ImportResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotResponse;
import de.ct.manager.schedule.maintenance.MaintenanceTaskRepository.StepCounts;
import de.ct.manager.schedule.maintenance.MaintenanceTaskRepository.TaskCounts;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaintenanceAircraftService {

    private final MaintenanceAircraftRepository aircraftRepository;
    private final MaintenanceTemplateRepository templateRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceDefectRepository defectRepository;
    private final MaintenanceWorkshopSlotRepository slotRepository;
    private final MaintenanceAccess access;

    public MaintenanceAircraftService(MaintenanceAircraftRepository aircraftRepository, MaintenanceTemplateRepository templateRepository,
            MaintenanceTaskRepository taskRepository, MaintenanceDefectRepository defectRepository,
            MaintenanceWorkshopSlotRepository slotRepository, MaintenanceAccess access) {
        this.aircraftRepository = aircraftRepository;
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.defectRepository = defectRepository;
        this.slotRepository = slotRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AircraftResponse> getAircraft() {
        Map<String, TaskCounts> tasks = taskRepository.countByAircraft().stream()
                .collect(Collectors.toMap(TaskCounts::getAircraftId, counts -> counts));
        Map<String, Long> openDefects = defectRepository.countOpenByAircraft().stream()
                .collect(Collectors.toMap(MaintenanceDefectRepository.OpenDefectCounts::getAircraftId, MaintenanceDefectRepository.OpenDefectCounts::getTotal));
        Map<String, long[]> steps = new HashMap<>();
        for (StepCounts counts : taskRepository.countChecklistStepsByAircraft()) {
            addSteps(steps, counts);
        }
        for (StepCounts counts : taskRepository.countTasksWithoutChecklistByAircraft()) {
            addSteps(steps, counts);
        }
        Map<String, WorkshopSlotResponse> workshops = currentOrNextWorkshops();

        return aircraftRepository.findAllByOrderByTypeAscRegistrationAsc().stream()
                .map(aircraft -> toResponse(aircraft, workshops.get(aircraft.getId()), tasks.get(aircraft.getId()),
                        steps.getOrDefault(aircraft.getId(), new long[2]), openDefects.getOrDefault(aircraft.getId(), 0L)))
                .toList();
    }

    private static void addSteps(Map<String, long[]> steps, StepCounts counts) {
        long[] sum = steps.computeIfAbsent(counts.getAircraftId(), id -> new long[2]);
        sum[0] += counts.getTotal();
        sum[1] += counts.getDone();
    }

    /** Per aircraft the workshop period running today or else the next one; past periods are ignored. */
    private Map<String, WorkshopSlotResponse> currentOrNextWorkshops() {
        LocalDate today = LocalDate.now();
        return slotRepository.findAllByOrderByStartDateAscEndDateAsc().stream()
                .filter(slot -> !slot.getEndDate().isBefore(today))
                .sorted(Comparator.comparing(MaintenanceWorkshopSlot::getStartDate))
                .collect(Collectors.toMap(slot -> slot.getAircraft().getId(), MaintenanceWorkshopService::toResponse, (first, second) -> first));
    }

    @Transactional(readOnly = true)
    public AircraftResponse getAircraft(String id) {
        return getAircraft().stream().filter(aircraft -> aircraft.id().equals(id)).findFirst()
                .orElseThrow(() -> MaintenanceAccess.notFound("Aircraft"));
    }

    @Transactional(readOnly = true)
    public List<String> getTypes() {
        return aircraftRepository.findAllTypes();
    }

    @Transactional
    public AircraftResponse create(AircraftRequest request, UserDetailsImpl currentUser) {
        String registration = request.registration().strip();
        if (aircraftRepository.existsByRegistrationIgnoreCase(registration)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already exists");
        }
        MaintenanceAircraft aircraft = new MaintenanceAircraft();
        apply(aircraft, request);
        MaintenanceAircraft saved = aircraftRepository.save(aircraft);
        log.info("User {} created aircraft {}", currentUser.getUsername(), saved.getRegistration());
        return toResponse(saved, null, null, new long[2], 0);
    }

    @Transactional
    public AircraftResponse update(String id, AircraftRequest request, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = find(id);
        if (aircraftRepository.existsByRegistrationIgnoreCaseAndIdNot(request.registration().strip(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already exists");
        }
        apply(aircraft, request);
        aircraftRepository.save(aircraft);
        log.info("User {} updated aircraft {}", currentUser.getUsername(), aircraft.getRegistration());
        return getAircraft(id);
    }

    /** Removes the aircraft together with its tasks and defects (cascaded by the database). */
    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = find(id);
        aircraftRepository.delete(aircraft);
        log.info("User {} deleted aircraft {}", currentUser.getUsername(), aircraft.getRegistration());
    }

    /** Creates a task for every template that applies to the aircraft and has no task on it yet. */
    @Transactional
    public ImportResponse importTemplates(String id, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = find(id);
        List<String> existing = taskRepository.findTemplateIdsByAircraft(aircraft);
        var actor = access.currentUser(currentUser);

        List<MaintenanceTask> tasks = templateRepository.findAllByOrderByLevelAscTitleAsc().stream()
                .filter(template -> template.appliesTo(aircraft.getType()) && !existing.contains(template.getId()))
                .map(template -> MaintenanceTaskService.fromTemplate(template, aircraft, actor))
                .toList();
        taskRepository.saveAll(tasks);
        log.info("User {} created {} tasks from templates for {}", currentUser.getUsername(), tasks.size(), aircraft.getRegistration());
        return new ImportResponse(tasks.size());
    }

    MaintenanceAircraft find(String id) {
        return aircraftRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Aircraft"));
    }

    private static void apply(MaintenanceAircraft aircraft, AircraftRequest request) {
        aircraft.setType(request.type().strip());
        aircraft.setRegistration(request.registration().strip());
    }

    private static AircraftResponse toResponse(MaintenanceAircraft aircraft, WorkshopSlotResponse workshop, TaskCounts tasks, long[] steps, long openDefects) {
        return new AircraftResponse(aircraft.getId(), aircraft.getType(), aircraft.getRegistration(), workshop,
                tasks == null ? 0 : tasks.getTotal(),
                tasks == null ? 0 : tasks.getDone(),
                tasks == null ? 0 : tasks.getSigned(),
                steps[0], steps[1],
                openDefects);
    }
}
