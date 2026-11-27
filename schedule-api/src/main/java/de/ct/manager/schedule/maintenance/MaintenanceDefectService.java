package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceAccess.Names;
import de.ct.manager.schedule.maintenance.MaintenanceDefectEventRepository.CommentCounts;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectDetail;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectEventResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskRef;
import de.ct.manager.schedule.notification.NotificationService;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * The defect book. Everybody reports defects, works on them (in progress, done, rejected, reopen) and comments.
 * Reporters, assignees and maintenance administrators edit a defect; maintenance administrators assign it to users,
 * sign off closed defects (which locks them) and delete any defect, reporters delete their own until it is signed off.
 */
@Service
@Slf4j
public class MaintenanceDefectService {

    private final MaintenanceDefectRepository defectRepository;
    private final MaintenanceDefectEventRepository eventRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceAircraftService aircraftService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MaintenanceAccess access;

    public MaintenanceDefectService(MaintenanceDefectRepository defectRepository, MaintenanceDefectEventRepository eventRepository,
            MaintenanceTaskRepository taskRepository, MaintenanceAircraftService aircraftService, UserRepository userRepository,
            NotificationService notificationService, MaintenanceAccess access) {
        this.defectRepository = defectRepository;
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.aircraftService = aircraftService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.access = access;
    }

    /** The defect book, of one aircraft or (aircraftId null) of all, latest first. */
    @Transactional(readOnly = true)
    public List<DefectResponse> getDefects(String aircraftId, UserDetailsImpl currentUser) {
        List<MaintenanceDefect> defects = aircraftId == null || aircraftId.isBlank()
                ? defectRepository.findAllByOrderByReportedAtDesc()
                : defectRepository.findAllByAircraftOrderByReportedAtDesc(aircraftService.find(aircraftId));
        if (defects.isEmpty()) {
            return List.of();
        }
        Map<String, MaintenanceTask> tasks = taskRepository.findAllByDefectIn(defects).stream()
                .collect(Collectors.toMap(task -> task.getDefect().getId(), task -> task));
        Map<String, Long> comments = eventRepository.countCommentsByDefect(defects).stream()
                .collect(Collectors.toMap(CommentCounts::getDefectId, CommentCounts::getTotal));
        Names names = access.names(defects.stream().flatMap(MaintenanceDefectService::people).toList());

        return defects.stream()
                .map(defect -> toResponse(defect, tasks.get(defect.getId()), comments.getOrDefault(defect.getId(), 0L), names, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public DefectDetail getDefect(String id, UserDetailsImpl currentUser) {
        return toDetail(find(id), currentUser);
    }

    /** Users a defect can be assigned to, by name. */
    @Transactional(readOnly = true)
    public List<Person> getAssignableUsers(UserDetailsImpl currentUser) {
        access.requireManage(currentUser);
        List<User> users = userRepository.findAll();
        Names names = access.names(users);
        return users.stream().map(names::of).sorted(Comparator.comparing(Person::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Transactional
    public DefectDetail create(String aircraftId, DefectRequest request, UserDetailsImpl currentUser) {
        return toDetail(report(aircraftService.find(aircraftId), request, currentUser), currentUser);
    }

    /** Reports a defect of the aircraft to the defect book, e.g. from an operations checklist. */
    @Transactional
    public MaintenanceDefect report(MaintenanceAircraft aircraft, DefectRequest request, UserDetailsImpl currentUser) {
        User actor = access.currentUser(currentUser);
        MaintenanceDefect defect = new MaintenanceDefect();
        defect.setAircraft(aircraft);
        defect.setReportedBy(actor);
        apply(defect, request);
        MaintenanceDefect saved = defectRepository.save(defect);
        log(saved, DefectEventAction.REPORTED, actor, null, null);
        if (request.assigneeIds() != null && !request.assigneeIds().isEmpty()) {
            access.requireManage(currentUser);
            assign(saved, request.assigneeIds(), actor);
        }
        log.info("User {} reported defect {} on {}", currentUser.getUsername(), saved.getTitle(), saved.getAircraft().getRegistration());
        return saved;
    }

    @Transactional
    public DefectDetail update(String id, DefectRequest request, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = findUnlocked(id);
        if (!isEditable(defect, currentUser)) {
            throw new AccessDeniedException("Only the reporter, assignees and maintenance administrators edit a defect");
        }
        User actor = access.currentUser(currentUser);
        String before = summary(defect);
        apply(defect, request);
        if (!before.equals(summary(defect))) {
            log(defect, DefectEventAction.UPDATED, actor, null, null);
        }
        if (request.assigneeIds() != null && !request.assigneeIds().equals(ids(defect.getAssignees()))) {
            access.requireManage(currentUser);
            assign(defect, request.assigneeIds(), actor);
        }
        return toDetail(defectRepository.save(defect), currentUser);
    }

    /**
     * Everybody works on a defect. Rejecting is a decision of the maintenance administrators (with a reason),
     * so only they reject a defect or take a rejection back.
     */
    @Transactional
    public DefectDetail setStatus(String id, DefectStatus status, String comment, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = findUnlocked(id);
        if (defect.getStatus() != status && (status == DefectStatus.REJECTED || defect.getStatus() == DefectStatus.REJECTED)) {
            access.requireManage(currentUser);
        }
        String reason = MaintenanceTaskService.blankToNull(comment);
        if (status == DefectStatus.REJECTED && reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejecting a defect needs a reason");
        }
        User actor = access.currentUser(currentUser);
        if (defect.getStatus() != status) {
            defect.setStatus(status);
            defect.setDoneBy(status.isClosed() ? actor : null);
            defect.setDoneAt(status.isClosed() ? Instant.now() : null);
            log(defect, switch (status) {
                case OPEN -> DefectEventAction.REOPENED;
                case IN_PROGRESS -> DefectEventAction.STARTED;
                case DONE -> DefectEventAction.DONE;
                case REJECTED -> DefectEventAction.REJECTED;
            }, actor, null, reason);
            log.info("User {} set defect {} to {}", currentUser.getUsername(), defect.getTitle(), status);
        } else if (reason != null) {
            log(defect, DefectEventAction.COMMENTED, actor, null, reason);
        }
        return toDetail(defectRepository.save(defect), currentUser);
    }

    @Transactional
    public DefectDetail addComment(String id, String content, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = findUnlocked(id);
        log(defect, DefectEventAction.COMMENTED, access.currentUser(currentUser), null, content.strip());
        return toDetail(defect, currentUser);
    }

    /** Authors delete their own comments, maintenance administrators all; the rest of the log stays. */
    @Transactional
    public DefectDetail deleteComment(String id, String eventId, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = findUnlocked(id);
        MaintenanceDefectEvent event = eventRepository.findById(eventId)
                .filter(found -> found.getDefect().getId().equals(id) && found.getAction() == DefectEventAction.COMMENTED)
                .orElseThrow(() -> MaintenanceAccess.notFound("Comment"));
        if (!isCommentDeletable(event, currentUser)) {
            throw new AccessDeniedException("Comment was written by another user");
        }
        eventRepository.delete(event);
        return toDetail(defect, currentUser);
    }

    /** Final acceptance of a done or rejected defect by a maintenance administrator; locks the defect. */
    @Transactional
    public DefectDetail signOff(String id, UserDetailsImpl currentUser) {
        access.requireManage(currentUser);
        MaintenanceDefect defect = find(id);
        if (defect.getSignedAt() == null) {
            if (!defect.getStatus().isClosed()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only done or rejected defects can be signed off");
            }
            User actor = access.currentUser(currentUser);
            defect.setSignedBy(actor);
            defect.setSignedAt(Instant.now());
            log(defect, DefectEventAction.SIGNED, actor, null, null);
            log.info("User {} signed off defect {}", currentUser.getUsername(), defect.getTitle());
        }
        return toDetail(defectRepository.save(defect), currentUser);
    }

    @Transactional
    public DefectDetail withdrawSignOff(String id, UserDetailsImpl currentUser) {
        access.requireManage(currentUser);
        MaintenanceDefect defect = find(id);
        if (defect.getSignedAt() != null) {
            defect.setSignedBy(null);
            defect.setSignedAt(null);
            log(defect, DefectEventAction.UNSIGNED, access.currentUser(currentUser), null, null);
        }
        return toDetail(defectRepository.save(defect), currentUser);
    }

    /** A linked task stays, it only loses the link. */
    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceDefect defect = find(id);
        if (!isDeletable(defect, currentUser)) {
            throw new AccessDeniedException("Only the reporter (until it is signed off) and maintenance administrators delete a defect");
        }
        taskRepository.findByDefect(defect).ifPresent(task -> task.setDefect(null));
        defectRepository.delete(defect);
        log.info("User {} deleted defect {}", currentUser.getUsername(), defect.getTitle());
    }

    // --- helpers

    private MaintenanceDefect find(String id) {
        return defectRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Defect"));
    }

    private MaintenanceDefect findUnlocked(String id) {
        MaintenanceDefect defect = find(id);
        if (defect.getSignedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Defect is signed off");
        }
        return defect;
    }

    /** Replaces the assignees, logs every change and notifies the users who were added. */
    private void assign(MaintenanceDefect defect, Set<String> userIds, User actor) {
        Set<User> wanted = new HashSet<>(userRepository.findAllById(userIds));
        if (wanted.size() != userIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user");
        }
        Set<String> before = ids(defect.getAssignees());
        Set<String> after = ids(wanted);
        defect.getAssignees().stream().filter(user -> !after.contains(user.getId())).toList().forEach(removed -> {
            defect.getAssignees().remove(removed);
            log(defect, DefectEventAction.UNASSIGNED, actor, removed, null);
            notificationService.defectUnassigned(defect, removed);
        });
        wanted.stream().filter(user -> !before.contains(user.getId())).sorted(Comparator.comparing(User::getUsername)).forEach(added -> {
            defect.getAssignees().add(added);
            log(defect, DefectEventAction.ASSIGNED, actor, added, null);
            notificationService.defectAssigned(defect, added, actor);
        });
    }

    private void log(MaintenanceDefect defect, DefectEventAction action, User actor, User subject, String content) {
        MaintenanceDefectEvent event = new MaintenanceDefectEvent();
        event.setDefect(defect);
        event.setAction(action);
        event.setActor(actor);
        event.setSubject(subject);
        event.setContent(content);
        eventRepository.save(event);
    }

    private static Set<String> ids(Collection<User> users) {
        return users.stream().map(User::getId).collect(Collectors.toSet());
    }

    private static String summary(MaintenanceDefect defect) {
        return defect.getTitle() + '\u0000' + defect.getDescription() + '\u0000' + defect.getPriority();
    }

    private static Stream<User> people(MaintenanceDefect defect) {
        return Stream.concat(Stream.of(defect.getReportedBy(), defect.getDoneBy(), defect.getSignedBy()), defect.getAssignees().stream());
    }

    private static boolean isAssigned(MaintenanceDefect defect, UserDetailsImpl currentUser) {
        return defect.getAssignees().stream().anyMatch(user -> MaintenanceAccess.isSameUser(user, currentUser));
    }

    private boolean isEditable(MaintenanceDefect defect, UserDetailsImpl currentUser) {
        return defect.getSignedAt() == null && (MaintenanceAccess.isSameUser(defect.getReportedBy(), currentUser)
                || isAssigned(defect, currentUser) || access.canManage(currentUser));
    }

    private boolean isDeletable(MaintenanceDefect defect, UserDetailsImpl currentUser) {
        return access.canManage(currentUser) || (defect.getSignedAt() == null && MaintenanceAccess.isSameUser(defect.getReportedBy(), currentUser));
    }

    private boolean isCommentDeletable(MaintenanceDefectEvent event, UserDetailsImpl currentUser) {
        return event.getAction() == DefectEventAction.COMMENTED && event.getDefect().getSignedAt() == null
                && (MaintenanceAccess.isSameUser(event.getActor(), currentUser) || access.canManage(currentUser));
    }

    private static void apply(MaintenanceDefect defect, DefectRequest request) {
        defect.setTitle(request.title().strip());
        defect.setDescription(MaintenanceTaskService.blankToNull(request.description()));
        defect.setPriority(request.priority());
    }

    private DefectDetail toDetail(MaintenanceDefect defect, UserDetailsImpl currentUser) {
        List<MaintenanceDefectEvent> events = eventRepository.findAllByDefectOrderByCreatedAtAscIdAsc(defect);
        Names names = access.names(Stream.concat(people(defect),
                events.stream().flatMap(event -> Stream.of(event.getActor(), event.getSubject()))).toList());
        long comments = events.stream().filter(event -> event.getAction() == DefectEventAction.COMMENTED).count();
        DefectResponse response = toResponse(defect, taskRepository.findByDefect(defect).orElse(null), comments, names, currentUser);
        return new DefectDetail(response, events.stream()
                .map(event -> new DefectEventResponse(event.getId(), event.getAction(), event.getContent(), names.of(event.getActor()),
                        names.of(event.getSubject()), event.getCreatedAt(), isCommentDeletable(event, currentUser)))
                .toList());
    }

    private DefectResponse toResponse(MaintenanceDefect defect, MaintenanceTask task, long commentCount, Names names, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = defect.getAircraft();
        List<Person> assignees = defect.getAssignees().stream().map(names::of).filter(Objects::nonNull)
                .sorted(Comparator.comparing(Person::name, String.CASE_INSENSITIVE_ORDER)).toList();
        return new DefectResponse(defect.getId(), aircraft.getId(), aircraft.getRegistration() + " · " + aircraft.getType(),
                defect.getTitle(), defect.getDescription(), defect.getPriority(), defect.getStatus(),
                names.of(defect.getReportedBy()), defect.getReportedAt(), names.of(defect.getDoneBy()), defect.getDoneAt(),
                names.of(defect.getSignedBy()), defect.getSignedAt(), assignees, isAssigned(defect, currentUser), commentCount,
                task == null ? null : new TaskRef(task.getId(), task.getTitle(), task.getStatus()),
                isEditable(defect, currentUser), isDeletable(defect, currentUser));
    }
}
