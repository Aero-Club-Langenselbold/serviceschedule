package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceAccess.Names;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.ChecklistItemResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRef;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.HistoryResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.NoteResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskDetail;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TaskSummary;
import de.ct.manager.schedule.maintenance.MaintenanceTaskRepository.ChecklistCounts;
import de.ct.manager.schedule.maintenance.MaintenanceTaskRepository.NoteCounts;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * Kanban cards. Every signed in user works on tasks (start, finish, checklist, notes),
 * maintenance administrators create, edit and delete them and sign them off.
 */
@Service
@Slf4j
public class MaintenanceTaskService {

    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceDefectRepository defectRepository;
    private final MaintenanceAircraftService aircraftService;
    private final MaintenanceWorkshopService workshopService;
    private final MaintenanceAccess access;

    public MaintenanceTaskService(MaintenanceTaskRepository taskRepository, MaintenanceDefectRepository defectRepository,
            MaintenanceAircraftService aircraftService, MaintenanceWorkshopService workshopService, MaintenanceAccess access) {
        this.taskRepository = taskRepository;
        this.defectRepository = defectRepository;
        this.aircraftService = aircraftService;
        this.workshopService = workshopService;
        this.access = access;
    }

    // --- reading

    /** The board of a workshop visit of the aircraft, or without workshopId the board without period. */
    @Transactional(readOnly = true)
    public List<TaskSummary> getTasks(String aircraftId, String workshopId) {
        MaintenanceAircraft aircraft = aircraftService.find(aircraftId);
        MaintenanceWorkshopSlot slot = workshopService.resolve(aircraft, workshopId);
        List<MaintenanceTask> tasks = slot == null
                ? taskRepository.findAllByAircraftAndWorkshopSlotIsNullOrderByLevelAscTitleAsc(aircraft)
                : taskRepository.findAllByWorkshopSlotOrderByLevelAscTitleAsc(slot);
        Map<String, ChecklistCounts> checklists = taskRepository.countChecklistByAircraft(aircraft).stream()
                .collect(Collectors.toMap(ChecklistCounts::getTaskId, Function.identity()));
        Map<String, Long> notes = taskRepository.countNotesByAircraft(aircraft).stream()
                .collect(Collectors.toMap(NoteCounts::getTaskId, NoteCounts::getTotal));
        Names names = access.names(tasks.stream().flatMap(task -> Stream.of(task.getDoneBy(), task.getSignedBy())).toList());

        return tasks.stream().map(task -> {
            ChecklistCounts checklist = checklists.get(task.getId());
            return new TaskSummary(task.getId(), aircraft.getId(), slotId(task), task.getTemplate() == null ? null : task.getTemplate().getId(), task.getTitle(), task.getDescription(), task.getLevel(),
                    task.getStatus(), task.getDurationMinutes(), task.getDeadline(), task.getTemplate() != null, defectRef(task.getDefect()),
                    checklist == null ? 0 : checklist.getTotal(), checklist == null ? 0 : checklist.getDone(),
                    notes.getOrDefault(task.getId(), 0L),
                    names.of(task.getDoneBy()), task.getDoneAt(), names.of(task.getSignedBy()), task.getSignedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public TaskDetail getTask(String id, UserDetailsImpl currentUser) {
        return toDetail(find(id), currentUser);
    }

    // --- administration

    @Transactional
    public TaskDetail create(String aircraftId, TaskRequest request, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = aircraftService.find(aircraftId);
        User actor = access.currentUser(currentUser);

        MaintenanceTask task = new MaintenanceTask();
        task.setAircraft(aircraft);
        apply(task, request);
        task.log(TaskHistoryAction.CREATED, actor);
        MaintenanceTask saved = taskRepository.save(task);
        log.info("User {} created task {} on {}", currentUser.getUsername(), saved.getTitle(), aircraft.getRegistration());
        return toDetail(saved, currentUser);
    }

    @Transactional
    public TaskDetail update(String id, TaskRequest request, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        User actor = access.currentUser(currentUser);
        apply(task, request);
        task.log(TaskHistoryAction.UPDATED, actor);
        // A new, unticked step means a finished task has work left again.
        if (task.getStatus() == TaskStatus.DONE && task.getChecklist().stream().anyMatch(item -> !item.isDone())) {
            markReopened(task, actor);
        }
        return toDetail(taskRepository.save(task), currentUser);
    }

    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceTask task = find(id);
        taskRepository.delete(task);
        log.info("User {} deleted task {}", currentUser.getUsername(), task.getTitle());
    }

    // --- status

    @Transactional
    public TaskDetail start(String id, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        requireStatus(task, TaskStatus.OPEN);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.log(TaskHistoryAction.STARTED, access.currentUser(currentUser));
        return toDetail(taskRepository.save(task), currentUser);
    }

    @Transactional
    public TaskDetail reset(String id, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        requireStatus(task, TaskStatus.IN_PROGRESS);
        task.setStatus(TaskStatus.OPEN);
        task.log(TaskHistoryAction.RESET, access.currentUser(currentUser));
        return toDetail(taskRepository.save(task), currentUser);
    }

    @Transactional
    public TaskDetail finish(String id, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        requireStatus(task, TaskStatus.IN_PROGRESS);
        markDone(task, access.currentUser(currentUser));
        return toDetail(taskRepository.save(task), currentUser);
    }

    @Transactional
    public TaskDetail reopen(String id, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        requireStatus(task, TaskStatus.DONE);
        markReopened(task, access.currentUser(currentUser));
        return toDetail(taskRepository.save(task), currentUser);
    }

    /**
     * Moves a task to any column of the board (drag and drop). Jumps over a column are logged as the
     * single steps they consist of, so the history reads the same as with the buttons.
     */
    @Transactional
    public TaskDetail moveTo(String id, TaskStatus target, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        User actor = access.currentUser(currentUser);
        while (task.getStatus() != target) {
            switch (task.getStatus()) {
                case OPEN -> {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    task.log(TaskHistoryAction.STARTED, actor);
                }
                case IN_PROGRESS -> {
                    if (target == TaskStatus.DONE) {
                        markDone(task, actor);
                    } else {
                        task.setStatus(TaskStatus.OPEN);
                        task.log(TaskHistoryAction.RESET, actor);
                    }
                }
                case DONE -> markReopened(task, actor);
            }
        }
        return toDetail(taskRepository.save(task), currentUser);
    }

    @Transactional
    public TaskDetail sign(String id, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        requireStatus(task, TaskStatus.DONE);
        User actor = access.currentUser(currentUser);
        // signing off confirms the whole task, so no step can stay open (e.g. of a task finished before this rule)
        completeChecklist(task, actor);
        task.setSignedBy(actor);
        task.setSignedAt(Instant.now());
        task.log(TaskHistoryAction.SIGNED, actor);
        return toDetail(taskRepository.save(task), currentUser);
    }

    @Transactional
    public TaskDetail unsign(String id, UserDetailsImpl currentUser) {
        MaintenanceTask task = find(id);
        if (!task.isSigned()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task is not signed off");
        }
        task.setSignedBy(null);
        task.setSignedAt(null);
        task.log(TaskHistoryAction.UNSIGNED, access.currentUser(currentUser));
        return toDetail(taskRepository.save(task), currentUser);
    }

    /** The checklist drives the status: the first tick starts the task, the last one finishes it, unticking reopens it. */
    @Transactional
    public TaskDetail toggleChecklistItem(String id, String itemId, boolean done, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        MaintenanceTaskChecklistItem item = task.getChecklist().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> MaintenanceAccess.notFound("Checklist item"));
        User actor = access.currentUser(currentUser);

        if (item.isDone() != done) {
            item.setDone(done);
            item.setDoneBy(done ? actor : null);
            item.setDoneAt(done ? Instant.now() : null);

            boolean allDone = task.getChecklist().stream().allMatch(MaintenanceTaskChecklistItem::isDone);
            if (done && task.getStatus() == TaskStatus.OPEN) {
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.log(TaskHistoryAction.STARTED, actor);
            }
            if (allDone && task.getStatus() == TaskStatus.IN_PROGRESS) {
                markDone(task, actor);
            } else if (!done && task.getStatus() == TaskStatus.DONE) {
                markReopened(task, actor);
            }
        }
        return toDetail(taskRepository.save(task), currentUser);
    }

    // --- notes

    @Transactional
    public TaskDetail addNote(String id, String content, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        User actor = access.currentUser(currentUser);

        MaintenanceTaskNote note = new MaintenanceTaskNote();
        note.setTask(task);
        note.setAuthor(actor);
        note.setContent(content.strip());
        task.getNotes().add(note);
        task.log(TaskHistoryAction.NOTE_ADDED, actor);
        return toDetail(taskRepository.save(task), currentUser);
    }

    /** Authors delete their own notes, maintenance administrators all of them. */
    @Transactional
    public TaskDetail deleteNote(String id, String noteId, UserDetailsImpl currentUser) {
        MaintenanceTask task = findUnlocked(id);
        MaintenanceTaskNote note = task.getNotes().stream().filter(candidate -> candidate.getId().equals(noteId)).findFirst()
                .orElseThrow(() -> MaintenanceAccess.notFound("Note"));
        if (!canDeleteNote(note, currentUser)) {
            throw new AccessDeniedException("Note belongs to another user");
        }
        task.getNotes().remove(note);
        task.log(TaskHistoryAction.NOTE_DELETED, access.currentUser(currentUser));
        return toDetail(taskRepository.save(task), currentUser);
    }

    // --- helpers

    static MaintenanceTask fromTemplate(MaintenanceTemplate template, MaintenanceAircraft aircraft, MaintenanceWorkshopSlot slot, User actor) {
        MaintenanceTask task = new MaintenanceTask();
        task.setAircraft(aircraft);
        task.setWorkshopSlot(slot);
        task.setTemplate(template);
        task.setTitle(template.getTitle());
        task.setDescription(template.getDescription());
        task.setLevel(template.getLevel());
        task.setDurationMinutes(template.getDurationMinutes());
        replaceChecklist(task, template.getChecklist());
        task.log(TaskHistoryAction.CREATED, actor);
        return task;
    }

    static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    static List<String> cleanChecklist(List<String> checklist) {
        return checklist == null ? List.of() : checklist.stream().filter(Objects::nonNull).map(String::strip).filter(text -> !text.isEmpty()).toList();
    }

    private void apply(MaintenanceTask task, TaskRequest request) {
        task.setTitle(request.title().strip());
        task.setDescription(blankToNull(request.description()));
        task.setLevel(request.level());
        task.setDurationMinutes(request.durationMinutes());
        task.setDeadline(request.deadline());
        task.setWorkshopSlot(workshopService.resolve(task.getAircraft(), request.workshopId()));
        task.setDefect(resolveDefect(task, request.defectId()));
        replaceChecklist(task, cleanChecklist(request.checklist()));
    }

    /** A task resolves at most one defect of its own aircraft, and a defect is resolved by at most one task. */
    private MaintenanceDefect resolveDefect(MaintenanceTask task, String defectId) {
        if (defectId == null || defectId.isBlank()) {
            return null;
        }
        MaintenanceDefect defect = defectRepository.findById(defectId).orElseThrow(() -> MaintenanceAccess.notFound("Defect"));
        if (!defect.getAircraft().getId().equals(task.getAircraft().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Defect belongs to another aircraft");
        }
        Optional<MaintenanceTask> linked = taskRepository.findByDefect(defect);
        if (linked.isPresent() && !linked.get().getId().equals(task.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Defect is already linked to another task");
        }
        return defect;
    }

    /**
     * Items keep their position, so rows are updated in place instead of being deleted and inserted again
     * (which would clash with the unique position per task). A step stays ticked as long as its text stays the same.
     */
    private static void replaceChecklist(MaintenanceTask task, List<String> texts) {
        record Snapshot(String text, boolean done, User doneBy, Instant doneAt) {
        }
        List<MaintenanceTaskChecklistItem> items = task.getChecklist();
        List<Snapshot> previous = new ArrayList<>(items.stream()
                .map(item -> new Snapshot(item.getText(), item.isDone(), item.getDoneBy(), item.getDoneAt())).toList());

        for (int position = 0; position < texts.size(); position++) {
            String text = texts.get(position);
            Snapshot match = previous.stream().filter(snapshot -> snapshot.text().equals(text)).findFirst().orElse(null);
            previous.remove(match);

            MaintenanceTaskChecklistItem item;
            if (position < items.size()) {
                item = items.get(position);
            } else {
                item = new MaintenanceTaskChecklistItem();
                item.setTask(task);
                item.setPosition(position);
                items.add(item);
            }
            item.setText(text);
            item.setDone(match != null && match.done());
            item.setDoneBy(match != null ? match.doneBy() : null);
            item.setDoneAt(match != null ? match.doneAt() : null);
        }
        while (items.size() > texts.size()) {
            items.remove(items.size() - 1);
        }
    }

    /** A finished task has no open steps: finishing it (button or drag and drop) ticks the remaining ones. */
    private static void markDone(MaintenanceTask task, User actor) {
        completeChecklist(task, actor);
        task.setStatus(TaskStatus.DONE);
        task.setDoneBy(actor);
        task.setDoneAt(Instant.now());
        task.log(TaskHistoryAction.FINISHED, actor);
    }

    private static void completeChecklist(MaintenanceTask task, User actor) {
        Instant now = Instant.now();
        for (MaintenanceTaskChecklistItem item : task.getChecklist()) {
            if (!item.isDone()) {
                item.setDone(true);
                item.setDoneBy(actor);
                item.setDoneAt(now);
            }
        }
    }

    private static void markReopened(MaintenanceTask task, User actor) {
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDoneBy(null);
        task.setDoneAt(null);
        task.log(TaskHistoryAction.REOPENED, actor);
    }

    private static void requireStatus(MaintenanceTask task, TaskStatus status) {
        if (task.getStatus() != status) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task is " + task.getStatus() + ", expected " + status);
        }
    }

    private MaintenanceTask find(String id) {
        return taskRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Task"));
    }

    private MaintenanceTask findUnlocked(String id) {
        MaintenanceTask task = find(id);
        if (task.isSigned()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task is signed off and locked");
        }
        return task;
    }

    private boolean canDeleteNote(MaintenanceTaskNote note, UserDetailsImpl currentUser) {
        return MaintenanceAccess.isSameUser(note.getAuthor(), currentUser) || access.canManage(currentUser);
    }

    private static String slotId(MaintenanceTask task) {
        return task.getWorkshopSlot() == null ? null : task.getWorkshopSlot().getId();
    }

    private static DefectRef defectRef(MaintenanceDefect defect) {
        return defect == null ? null : new DefectRef(defect.getId(), defect.getTitle(), defect.getStatus());
    }

    private TaskDetail toDetail(MaintenanceTask task, UserDetailsImpl currentUser) {
        Names names = access.names(Stream.of(
                Stream.of(task.getDoneBy(), task.getSignedBy()),
                task.getChecklist().stream().map(MaintenanceTaskChecklistItem::getDoneBy),
                task.getNotes().stream().map(MaintenanceTaskNote::getAuthor),
                task.getHistory().stream().map(MaintenanceTaskHistory::getActor))
                .flatMap(Function.identity()).toList());
        boolean locked = task.isSigned();
        MaintenanceAircraft aircraft = task.getAircraft();

        return new TaskDetail(task.getId(), aircraft.getId(), aircraft.getType() + " · " + aircraft.getRegistration(), slotId(task),
                task.getTitle(), task.getDescription(), task.getLevel(), task.getStatus(), task.getDurationMinutes(), task.getDeadline(),
                task.getTemplate() != null, defectRef(task.getDefect()),
                names.of(task.getDoneBy()), task.getDoneAt(), names.of(task.getSignedBy()), task.getSignedAt(),
                task.getChecklist().stream()
                        .map(item -> new ChecklistItemResponse(item.getId(), item.getText(), item.isDone(), names.of(item.getDoneBy()), item.getDoneAt()))
                        .toList(),
                task.getNotes().stream()
                        .map(note -> new NoteResponse(note.getId(), note.getContent(), names.of(note.getAuthor()), note.getCreatedAt(),
                                !locked && canDeleteNote(note, currentUser)))
                        .toList(),
                task.getHistory().stream()
                        .map(entry -> new HistoryResponse(entry.getId(), entry.getAction(), names.of(entry.getActor()), entry.getCreatedAt()))
                        .toList());
    }
}
