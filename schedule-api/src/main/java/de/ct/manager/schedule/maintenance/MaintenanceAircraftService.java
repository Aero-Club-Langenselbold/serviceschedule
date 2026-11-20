package de.ct.manager.schedule.maintenance;

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
    private final MaintenanceAccess access;

    public MaintenanceAircraftService(MaintenanceAircraftRepository aircraftRepository, MaintenanceTemplateRepository templateRepository,
            MaintenanceTaskRepository taskRepository, MaintenanceDefectRepository defectRepository, MaintenanceAccess access) {
        this.aircraftRepository = aircraftRepository;
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.defectRepository = defectRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AircraftResponse> getAircraft() {
        Map<String, TaskCounts> tasks = taskRepository.countByAircraft().stream()
                .collect(Collectors.toMap(TaskCounts::getAircraftId, counts -> counts));
        Map<String, Long> openDefects = defectRepository.countOpenByAircraft().stream()
                .collect(Collectors.toMap(MaintenanceDefectRepository.OpenDefectCounts::getAircraftId, MaintenanceDefectRepository.OpenDefectCounts::getTotal));

        return aircraftRepository.findAllByOrderByTypeAscRegistrationAsc().stream()
                .map(aircraft -> toResponse(aircraft, tasks.get(aircraft.getId()), openDefects.getOrDefault(aircraft.getId(), 0L)))
                .toList();
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
        return toResponse(saved, null, 0);
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
        if (request.workshopStart() != null && request.workshopEnd() != null && request.workshopEnd().isBefore(request.workshopStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workshop period ends before it starts");
        }
        aircraft.setType(request.type().strip());
        aircraft.setRegistration(request.registration().strip());
        aircraft.setWorkshopStart(request.workshopStart());
        aircraft.setWorkshopEnd(request.workshopEnd());
    }

    private static AircraftResponse toResponse(MaintenanceAircraft aircraft, TaskCounts tasks, long openDefects) {
        return new AircraftResponse(aircraft.getId(), aircraft.getType(), aircraft.getRegistration(),
                aircraft.getWorkshopStart(), aircraft.getWorkshopEnd(),
                tasks == null ? 0 : tasks.getTotal(),
                tasks == null ? 0 : tasks.getDone(),
                tasks == null ? 0 : tasks.getSigned(),
                openDefects);
    }
}
