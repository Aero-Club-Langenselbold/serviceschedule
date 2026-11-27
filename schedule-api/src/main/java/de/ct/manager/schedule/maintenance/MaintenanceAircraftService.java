package de.ct.manager.schedule.maintenance;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.AircraftRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.AircraftResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.ImportResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TemplateImportRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.WorkshopSlotResponse;
import de.ct.manager.schedule.maintenance.MaintenanceTaskRepository.UnscheduledCounts;
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
    private final MaintenanceWorkshopService workshopService;
    private final MaintenanceAccess access;

    public MaintenanceAircraftService(MaintenanceAircraftRepository aircraftRepository, MaintenanceTemplateRepository templateRepository,
            MaintenanceTaskRepository taskRepository, MaintenanceDefectRepository defectRepository,
            MaintenanceWorkshopSlotRepository slotRepository, MaintenanceWorkshopService workshopService, MaintenanceAccess access) {
        this.aircraftRepository = aircraftRepository;
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.defectRepository = defectRepository;
        this.slotRepository = slotRepository;
        this.workshopService = workshopService;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AircraftResponse> getAircraft() {
        Map<String, Long> unscheduled = taskRepository.countUnscheduledByAircraft().stream()
                .collect(Collectors.toMap(UnscheduledCounts::getAircraftId, UnscheduledCounts::getTotal));
        Map<String, Long> openDefects = defectRepository.countOpenByAircraft().stream()
                .collect(Collectors.toMap(MaintenanceDefectRepository.OpenDefectCounts::getAircraftId, MaintenanceDefectRepository.OpenDefectCounts::getTotal));
        Map<String, WorkshopSlotResponse> workshops = currentOrNextWorkshops();

        return aircraftRepository.findAllByOrderByTypeAscRegistrationAsc().stream()
                .map(aircraft -> toResponse(aircraft, workshops.get(aircraft.getId()), unscheduled.getOrDefault(aircraft.getId(), 0L),
                        openDefects.getOrDefault(aircraft.getId(), 0L)))
                .toList();
    }

    /** Per aircraft the workshop period running today or else the next one; past periods are ignored. */
    private Map<String, WorkshopSlotResponse> currentOrNextWorkshops() {
        LocalDate today = LocalDate.now();
        Map<String, MaintenanceWorkshopService.Progress> progress = workshopService.progressBySlot();
        return slotRepository.findAllByOrderByStartDateAscEndDateAsc().stream()
                .filter(slot -> !slot.getEndDate().isBefore(today))
                .collect(Collectors.toMap(slot -> slot.getAircraft().getId(),
                        slot -> MaintenanceWorkshopService.toResponse(slot, progress.get(slot.getId())), (first, second) -> first));
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
        return toResponse(saved, null, 0, 0);
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

    /**
     * Creates a task from each chosen template on the board of the workshop visit. Templates must apply to the aircraft;
     * templates that already have a task on that board are skipped.
     */
    @Transactional
    public ImportResponse importTemplates(String id, TemplateImportRequest request, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = find(id);
        MaintenanceWorkshopSlot slot = workshopService.resolve(aircraft, request.workshopId());
        List<String> existing = slot == null ? taskRepository.findUnscheduledTemplateIdsByAircraft(aircraft) : taskRepository.findTemplateIdsByWorkshopSlot(slot);
        Set<String> chosen = new LinkedHashSet<>(request.templateIds());
        List<MaintenanceTemplate> templates = templateRepository.findAllById(chosen);
        if (templates.size() != chosen.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown template");
        }
        if (templates.stream().anyMatch(template -> !template.appliesTo(aircraft.getType()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template does not apply to the aircraft type");
        }
        var actor = access.currentUser(currentUser);

        List<MaintenanceTask> tasks = templates.stream()
                .filter(template -> !existing.contains(template.getId()))
                .map(template -> MaintenanceTaskService.fromTemplate(template, aircraft, slot, actor))
                .toList();
        taskRepository.saveAll(tasks);
        log.info("User {} created {} tasks from templates for {}", currentUser.getUsername(), tasks.size(), aircraft.getRegistration());
        return new ImportResponse(tasks.size());
    }

    public MaintenanceAircraft find(String id) {
        return aircraftRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Aircraft"));
    }

    private static void apply(MaintenanceAircraft aircraft, AircraftRequest request) {
        aircraft.setType(request.type().strip());
        aircraft.setRegistration(request.registration().strip());
    }

    private static AircraftResponse toResponse(MaintenanceAircraft aircraft, WorkshopSlotResponse workshop, long unscheduledTasks, long openDefects) {
        return new AircraftResponse(aircraft.getId(), aircraft.getType(), aircraft.getRegistration(), workshop, unscheduledTasks, openDefects);
    }
}
