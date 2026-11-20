package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ct.manager.schedule.maintenance.MaintenanceAccess.Names;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskRef;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

/** Defects and findings: everybody reports them and marks them done, reporters and administrators edit or delete them. */
@Service
@Slf4j
public class MaintenanceDefectService {

    private final MaintenanceDefectRepository defectRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceAircraftService aircraftService;
    private final MaintenanceAccess access;

    public MaintenanceDefectService(MaintenanceDefectRepository defectRepository, MaintenanceTaskRepository taskRepository,
            MaintenanceAircraftService aircraftService, MaintenanceAccess access) {
        this.defectRepository = defectRepository;
        this.taskRepository = taskRepository;
        this.aircraftService = aircraftService;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<DefectResponse> getDefects(String aircraftId, UserDetailsImpl currentUser) {
        List<MaintenanceDefect> defects = defectRepository.findAllByAircraftOrderByReportedAtDesc(aircraftService.find(aircraftId));
        Map<String, MaintenanceTask> tasks = defects.isEmpty() ? Map.of() : taskRepository.findAllByDefectIn(defects).stream()
                .collect(Collectors.toMap(task -> task.getDefect().getId(), task -> task));
        Names names = access.names(defects.stream().flatMap(defect -> Stream.of(defect.getReportedBy(), defect.getDoneBy())).toList());

        return defects.stream().map(defect -> toResponse(defect, tasks.get(defect.getId()), names, currentUser)).toList();
    }

    @Transactional
    public DefectResponse create(String aircraftId, DefectRequest request, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = new MaintenanceDefect();
        defect.setAircraft(aircraftService.find(aircraftId));
        defect.setReportedBy(access.currentUser(currentUser));
        apply(defect, request);
        MaintenanceDefect saved = defectRepository.save(defect);
        log.info("User {} reported defect {} on {}", currentUser.getUsername(), saved.getTitle(), saved.getAircraft().getRegistration());
        return toResponse(saved, null, currentUser);
    }

    @Transactional
    public DefectResponse update(String id, DefectRequest request, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = findEditable(id, currentUser);
        apply(defect, request);
        return toResponse(defectRepository.save(defect), taskRepository.findByDefect(defect).orElse(null), currentUser);
    }

    @Transactional
    public DefectResponse setStatus(String id, DefectStatus status, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = find(id);
        if (defect.getStatus() != status) {
            User actor = access.currentUser(currentUser);
            defect.setStatus(status);
            defect.setDoneBy(status == DefectStatus.DONE ? actor : null);
            defect.setDoneAt(status == DefectStatus.DONE ? Instant.now() : null);
        }
        return toResponse(defectRepository.save(defect), taskRepository.findByDefect(defect).orElse(null), currentUser);
    }

    /** A linked task stays, it only loses the link. */
    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = findEditable(id, currentUser);
        taskRepository.findByDefect(defect).ifPresent(task -> task.setDefect(null));
        defectRepository.delete(defect);
        log.info("User {} deleted defect {}", currentUser.getUsername(), defect.getTitle());
    }

    private MaintenanceDefect find(String id) {
        return defectRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Defect"));
    }

    private MaintenanceDefect findEditable(String id, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = find(id);
        if (!isEditable(defect, currentUser)) {
            throw new AccessDeniedException("Defect was reported by another user");
        }
        return defect;
    }

    private boolean isEditable(MaintenanceDefect defect, UserDetailsImpl currentUser) {
        return MaintenanceAccess.isSameUser(defect.getReportedBy(), currentUser) || access.canManage(currentUser);
    }

    private static void apply(MaintenanceDefect defect, DefectRequest request) {
        defect.setTitle(request.title().strip());
        defect.setDescription(MaintenanceTaskService.blankToNull(request.description()));
        defect.setPriority(request.priority());
    }

    private DefectResponse toResponse(MaintenanceDefect defect, MaintenanceTask task, UserDetailsImpl currentUser) {
        return toResponse(defect, task, access.names(Stream.of(defect.getReportedBy(), defect.getDoneBy()).toList()), currentUser);
    }

    private DefectResponse toResponse(MaintenanceDefect defect, MaintenanceTask task, Names names, UserDetailsImpl currentUser) {
        return new DefectResponse(defect.getId(), defect.getAircraft().getId(), defect.getTitle(), defect.getDescription(),
                defect.getPriority(), defect.getStatus(), names.of(defect.getReportedBy()), defect.getReportedAt(),
                names.of(defect.getDoneBy()), defect.getDoneAt(),
                task == null ? null : new TaskRef(task.getId(), task.getTitle(), task.getStatus()),
                isEditable(defect, currentUser));
    }
}
