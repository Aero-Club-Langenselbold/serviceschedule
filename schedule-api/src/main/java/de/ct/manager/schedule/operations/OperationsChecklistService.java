package de.ct.manager.schedule.operations;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceAccess;
import de.ct.manager.schedule.maintenance.MaintenanceAccess.Names;
import de.ct.manager.schedule.maintenance.MaintenanceAircraft;
import de.ct.manager.schedule.maintenance.MaintenanceAircraftRepository;
import de.ct.manager.schedule.maintenance.MaintenanceAircraftService;
import de.ct.manager.schedule.maintenance.MaintenanceDefect;
import de.ct.manager.schedule.maintenance.MaintenanceDefectService;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRef;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRequest;
import de.ct.manager.schedule.notification.NotificationService;
import de.ct.manager.schedule.operations.OperationsChecklistItemRepository.Progress;
import de.ct.manager.schedule.operations.OperationsDtos.AircraftChecklists;
import de.ct.manager.schedule.operations.OperationsDtos.ChecklistDetail;
import de.ct.manager.schedule.operations.OperationsDtos.ChecklistSummary;
import de.ct.manager.schedule.operations.OperationsDtos.ItemRequest;
import de.ct.manager.schedule.operations.OperationsDtos.ItemResponse;
import de.ct.manager.schedule.operations.OperationsDtos.OverviewResponse;
import de.ct.manager.schedule.operations.OperationsDtos.TemplateRequest;
import de.ct.manager.schedule.operations.OperationsDtos.TemplateResponse;
import de.ct.manager.schedule.operations.OperationsDtos.TodayResponse;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * Checklists for the start and end of operations.
 * <ul>
 * <li>Maintenance and operations administrators maintain one template per aircraft and kind.</li>
 * <li>Every signed in member starts the checklist of today (at most one per aircraft, kind and day; the starter is
 * recorded as initial recorder for good), checks its items as OK or not OK, reports defects to the defect book and
 * finally confirms it complete, which notifies the operations administrators.</li>
 * <li>Operations administrators approve submitted checklists, send them back and search the archive. Members only see
 * the checklists of today.</li>
 * </ul>
 */
@Service
@Slf4j
public class OperationsChecklistService {

    private final OperationsChecklistTemplateRepository templateRepository;
    private final OperationsChecklistRepository checklistRepository;
    private final OperationsChecklistItemRepository itemRepository;
    private final MaintenanceAircraftRepository aircraftRepository;
    private final MaintenanceAircraftService aircraftService;
    private final MaintenanceDefectService defectService;
    private final NotificationService notificationService;
    private final MaintenanceAccess access;
    private final ZoneId zone;

    public OperationsChecklistService(OperationsChecklistTemplateRepository templateRepository, OperationsChecklistRepository checklistRepository,
            OperationsChecklistItemRepository itemRepository, MaintenanceAircraftRepository aircraftRepository,
            MaintenanceAircraftService aircraftService, MaintenanceDefectService defectService, NotificationService notificationService,
            MaintenanceAccess access, @Value("${schedule.calendar.timezone:Europe/Berlin}") String zone) {
        this.templateRepository = templateRepository;
        this.checklistRepository = checklistRepository;
        this.itemRepository = itemRepository;
        this.aircraftRepository = aircraftRepository;
        this.aircraftService = aircraftService;
        this.defectService = defectService;
        this.notificationService = notificationService;
        this.access = access;
        this.zone = ZoneId.of(zone);
    }

    /** The day of the club, independent of the time zone of the server. */
    public LocalDate today() {
        return LocalDate.now(zone);
    }

    // --- rights

    /** Maintenance and operations administrators maintain the templates. */
    public boolean canManageTemplates(UserDetailsImpl currentUser) {
        return access.canManage(currentUser) || access.canManageOperations(currentUser);
    }

    private void requireTemplates(UserDetailsImpl currentUser) {
        if (!canManageTemplates(currentUser)) {
            throw new AccessDeniedException("Maintenance and operations administrators only");
        }
    }

    private void requireOperations(UserDetailsImpl currentUser) {
        if (!access.canManageOperations(currentUser)) {
            throw new AccessDeniedException("Operations administrators only");
        }
    }

    // --- overview and templates

    @Transactional(readOnly = true)
    public OverviewResponse getOverview(UserDetailsImpl currentUser) {
        LocalDate today = today();
        Map<String, OperationsChecklistTemplate> templates = templateRepository.findAllBy().stream()
                .collect(Collectors.toMap(template -> key(template.getAircraft(), template.getKind()), Function.identity()));
        List<OperationsChecklist> checklists = checklistRepository.findAllByDate(today).stream()
                .filter(checklist -> !isOutdated(checklist, templates.get(key(checklist.getAircraft(), checklist.getKind()))))
                .toList();
        Map<String, ChecklistSummary> summaries = summaries(checklists).stream()
                .collect(Collectors.toMap(summary -> summary.aircraftId() + ':' + summary.kind(), Function.identity()));

        List<AircraftChecklists> aircraft = aircraftRepository.findAll().stream()
                .sorted(Comparator.comparing(MaintenanceAircraft::getRegistration, String.CASE_INSENSITIVE_ORDER))
                .map(found -> new AircraftChecklists(found.getId(), found.getRegistration(), found.getType(),
                        toTemplate(templates.get(key(found, ChecklistKind.START))), toTemplate(templates.get(key(found, ChecklistKind.END))),
                        summaries.get(key(found, ChecklistKind.START)), summaries.get(key(found, ChecklistKind.END))))
                .toList();
        return new OverviewResponse(today, aircraft);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(String aircraftId, ChecklistKind kind) {
        return templateRepository.findByAircraftAndKind(aircraftService.find(aircraftId), kind)
                .map(OperationsChecklistService::toTemplate)
                .orElseThrow(() -> MaintenanceAccess.notFound("Checklist template"));
    }

    /** Creates or replaces the template; checklists already started keep their items. */
    @Transactional
    public TemplateResponse saveTemplate(String aircraftId, ChecklistKind kind, TemplateRequest request, UserDetailsImpl currentUser) {
        requireTemplates(currentUser);
        MaintenanceAircraft aircraft = aircraftService.find(aircraftId);
        List<String> items = request.items().stream().map(String::strip).filter(text -> !text.isEmpty()).toList();
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A checklist needs at least one item");
        }
        OperationsChecklistTemplate template = templateRepository.findByAircraftAndKind(aircraft, kind).orElseGet(() -> {
            OperationsChecklistTemplate created = new OperationsChecklistTemplate();
            created.setAircraft(aircraft);
            created.setKind(kind);
            return created;
        });
        template.getItems().clear();
        template.getItems().addAll(items);
        template.setUpdatedAt(Instant.now());
        OperationsChecklistTemplate saved = templateRepository.save(template);
        log.info("User {} saved the {} checklist of {}", currentUser.getUsername(), kind, aircraft.getRegistration());
        return toTemplate(saved);
    }

    @Transactional
    public void deleteTemplate(String aircraftId, ChecklistKind kind, UserDetailsImpl currentUser) {
        requireTemplates(currentUser);
        MaintenanceAircraft aircraft = aircraftService.find(aircraftId);
        OperationsChecklistTemplate template = templateRepository.findByAircraftAndKind(aircraft, kind)
                .orElseThrow(() -> MaintenanceAccess.notFound("Checklist template"));
        templateRepository.delete(template);
        log.info("User {} deleted the {} checklist of {}", currentUser.getUsername(), kind, aircraft.getRegistration());
    }

    // --- the checklist of today

    /** Opening the checklist of today discards it if it is outdated (see {@link #isOutdated}). */
    @Transactional
    public TodayResponse getToday(String aircraftId, ChecklistKind kind, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = aircraftService.find(aircraftId);
        LocalDate today = today();
        OperationsChecklistTemplate template = templateRepository.findByAircraftAndKind(aircraft, kind).orElse(null);
        ChecklistDetail checklist = findCurrent(aircraft, kind, today, template, currentUser)
                .map(found -> toDetail(found, currentUser)).orElse(null);
        List<String> templateItems = template == null ? null : List.copyOf(template.getItems());
        return new TodayResponse(aircraft.getId(), aircraft.getRegistration(), aircraft.getType(), kind, today, templateItems, checklist);
    }

    /**
     * A checklist of today still in progress whose template was deleted and created anew is outdated: it no longer belongs
     * to the checklist that hangs on the aircraft. Submitted and approved checklists are records and never outdated, nor
     * is one whose template was only changed (that keeps the template) or deleted without a new one.
     */
    static boolean isOutdated(OperationsChecklist checklist, OperationsChecklistTemplate current) {
        return checklist.getStatus() == ChecklistStatus.IN_PROGRESS && current != null
                && (checklist.getTemplate() == null || !checklist.getTemplate().getId().equals(current.getId()));
    }

    /** The checklist of today, after discarding an outdated one; until it is opened, an outdated one stays in the archive. */
    private Optional<OperationsChecklist> findCurrent(MaintenanceAircraft aircraft, ChecklistKind kind, LocalDate today,
            OperationsChecklistTemplate template, UserDetailsImpl currentUser) {
        return checklistRepository.findByAircraftAndKindAndDate(aircraft, kind, today).filter(found -> {
            if (!isOutdated(found, template)) {
                return true;
            }
            checklistRepository.delete(found);
            checklistRepository.flush();
            log.info("User {} opened the outdated {} checklist of {} on {}, it was discarded", currentUser.getUsername(), kind,
                    aircraft.getRegistration(), today);
            return false;
        });
    }

    /** Starts the checklist of today from the template; the caller becomes the initial recorder. */
    @Transactional
    public ChecklistDetail start(String aircraftId, ChecklistKind kind, UserDetailsImpl currentUser) {
        MaintenanceAircraft aircraft = aircraftService.find(aircraftId);
        LocalDate today = today();
        OperationsChecklistTemplate template = templateRepository.findByAircraftAndKind(aircraft, kind)
                .orElseThrow(() -> MaintenanceAccess.notFound("Checklist template"));
        if (findCurrent(aircraft, kind, today, template, currentUser).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The checklist of today has already been started");
        }
        OperationsChecklist checklist = new OperationsChecklist();
        checklist.setAircraft(aircraft);
        checklist.setKind(kind);
        checklist.setDate(today);
        checklist.setTemplate(template);
        checklist.setCreatedBy(access.currentUser(currentUser));
        try {
            checklist = checklistRepository.saveAndFlush(checklist);
        } catch (DataIntegrityViolationException e) {
            // somebody else started it at the same moment
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The checklist of today has already been started");
        }
        List<OperationsChecklistItem> items = new ArrayList<>();
        for (String text : template.getItems()) {
            OperationsChecklistItem item = new OperationsChecklistItem();
            item.setChecklist(checklist);
            item.setPosition(items.size());
            item.setText(text);
            items.add(item);
        }
        itemRepository.saveAll(items);
        log.info("User {} started the {} checklist of {} on {}", currentUser.getUsername(), kind, aircraft.getRegistration(), today);
        return toDetail(checklist, currentUser);
    }

    @Transactional(readOnly = true)
    public ChecklistDetail getChecklist(String id, UserDetailsImpl currentUser) {
        OperationsChecklist checklist = find(id);
        if (!checklist.getDate().equals(today())) {
            requireOperations(currentUser);
        }
        return toDetail(checklist, currentUser);
    }

    /** Checks (OK / NOT_OK) or unchecks an item; whoever does it is recorded with the item. */
    @Transactional
    public ChecklistDetail checkItem(String id, String itemId, ItemRequest request, UserDetailsImpl currentUser) {
        OperationsChecklist checklist = findEditable(id);
        OperationsChecklistItem item = findItem(checklist, itemId);
        if (request.result() == null && item.getDefect() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An item with a reported defect stays not OK");
        }
        if (request.result() == ChecklistItemResult.OK && item.getDefect() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An item with a reported defect stays not OK");
        }
        item.setResult(request.result());
        item.setComment(blankToNull(request.comment()));
        item.setCheckedBy(request.result() == null ? null : access.currentUser(currentUser));
        item.setCheckedAt(request.result() == null ? null : Instant.now());
        itemRepository.save(item);
        return toDetail(checklist, currentUser);
    }

    /** Reports a defect of the aircraft to the defect book from an item, which is checked as not OK. */
    @Transactional
    public ChecklistDetail reportDefect(String id, String itemId, DefectRequest request, UserDetailsImpl currentUser) {
        OperationsChecklist checklist = findEditable(id);
        OperationsChecklistItem item = findItem(checklist, itemId);
        if (item.getDefect() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A defect has already been reported for this item");
        }
        if (request.assigneeIds() != null && !request.assigneeIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Defects are assigned in the defect book");
        }
        MaintenanceDefect defect = defectService.report(checklist.getAircraft(), request, currentUser);
        item.setDefect(defect);
        item.setResult(ChecklistItemResult.NOT_OK);
        item.setCheckedBy(access.currentUser(currentUser));
        item.setCheckedAt(Instant.now());
        itemRepository.save(item);
        return toDetail(checklist, currentUser);
    }

    /** Confirms the checklist complete (every item checked) and notifies the operations administrators. */
    @Transactional
    public ChecklistDetail submit(String id, UserDetailsImpl currentUser) {
        OperationsChecklist checklist = findEditable(id);
        if (itemRepository.findAllByChecklistOrderByPosition(checklist).stream().anyMatch(item -> item.getResult() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Every item has to be checked first");
        }
        User actor = access.currentUser(currentUser);
        checklist.setStatus(ChecklistStatus.SUBMITTED);
        checklist.setSubmittedBy(actor);
        checklist.setSubmittedAt(Instant.now());
        checklistRepository.save(checklist);
        notificationService.checklistSubmitted(checklist, actor);
        log.info("User {} submitted the {} checklist of {} on {}", currentUser.getUsername(), checklist.getKind(),
                checklist.getAircraft().getRegistration(), checklist.getDate());
        return toDetail(checklist, currentUser);
    }

    @Transactional
    public ChecklistDetail approve(String id, UserDetailsImpl currentUser) {
        requireOperations(currentUser);
        OperationsChecklist checklist = find(id);
        if (checklist.getStatus() != ChecklistStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only submitted checklists can be approved");
        }
        checklist.setStatus(ChecklistStatus.APPROVED);
        checklist.setApprovedBy(access.currentUser(currentUser));
        checklist.setApprovedAt(Instant.now());
        checklistRepository.save(checklist);
        notificationService.checklistDone(checklist);
        log.info("User {} approved the {} checklist of {} on {}", currentUser.getUsername(), checklist.getKind(),
                checklist.getAircraft().getRegistration(), checklist.getDate());
        return toDetail(checklist, currentUser);
    }

    /** Sends a submitted or approved checklist of today back to be filled in again. */
    @Transactional
    public ChecklistDetail reopen(String id, UserDetailsImpl currentUser) {
        requireOperations(currentUser);
        OperationsChecklist checklist = find(id);
        if (!isReopenable(checklist, currentUser)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only submitted or approved checklists of today can be reopened");
        }
        checklist.setStatus(ChecklistStatus.IN_PROGRESS);
        checklist.setSubmittedBy(null);
        checklist.setSubmittedAt(null);
        checklist.setApprovedBy(null);
        checklist.setApprovedAt(null);
        checklistRepository.save(checklist);
        notificationService.checklistReopened(checklist);
        return toDetail(checklist, currentUser);
    }

    // --- archive

    @Transactional(readOnly = true)
    public Page<ChecklistSummary> search(String aircraftId, ChecklistKind kind, ChecklistStatus status, LocalDate from, LocalDate to,
            String search, Pageable pageable, UserDetailsImpl currentUser) {
        requireOperations(currentUser);
        String pattern = search == null || search.isBlank() ? null : '%' + search.strip().toLowerCase() + '%';
        Page<OperationsChecklist> page = checklistRepository.search(blankToNull(aircraftId), kind, status, from, to, pattern, pageable);
        Map<String, ChecklistSummary> summaries = summaries(page.getContent()).stream()
                .collect(Collectors.toMap(ChecklistSummary::id, Function.identity()));
        return page.map(checklist -> summaries.get(checklist.getId()));
    }

    // --- helpers

    private OperationsChecklist find(String id) {
        return checklistRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Checklist"));
    }

    /** Only the checklist of today is filled in, and only until it is submitted. */
    private OperationsChecklist findEditable(String id) {
        OperationsChecklist checklist = find(id);
        if (!isEditable(checklist)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only the checklist of today can be filled in until it is submitted");
        }
        return checklist;
    }

    private OperationsChecklistItem findItem(OperationsChecklist checklist, String itemId) {
        return itemRepository.findById(itemId)
                .filter(item -> item.getChecklist().getId().equals(checklist.getId()))
                .orElseThrow(() -> MaintenanceAccess.notFound("Checklist item"));
    }

    private boolean isEditable(OperationsChecklist checklist) {
        return checklist.getStatus() == ChecklistStatus.IN_PROGRESS && checklist.getDate().equals(today());
    }

    private boolean isReopenable(OperationsChecklist checklist, UserDetailsImpl currentUser) {
        return checklist.getStatus() != ChecklistStatus.IN_PROGRESS && checklist.getDate().equals(today()) && access.canManageOperations(currentUser);
    }

    private static String key(MaintenanceAircraft aircraft, ChecklistKind kind) {
        return aircraft.getId() + ':' + kind;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static TemplateResponse toTemplate(OperationsChecklistTemplate template) {
        return template == null ? null : new TemplateResponse(template.getId(), template.getAircraft().getId(), template.getKind(),
                List.copyOf(template.getItems()), template.getUpdatedAt());
    }

    private List<ChecklistSummary> summaries(List<OperationsChecklist> checklists) {
        if (checklists.isEmpty()) {
            return List.of();
        }
        Map<String, Progress> progress = itemRepository.progressOf(checklists).stream()
                .collect(Collectors.toMap(Progress::getChecklistId, Function.identity()));
        Names names = access.names(checklists.stream().flatMap(OperationsChecklistService::people).toList());
        return checklists.stream().map(checklist -> toSummary(checklist, progress.get(checklist.getId()), names)).toList();
    }

    private static Stream<User> people(OperationsChecklist checklist) {
        return Stream.of(checklist.getCreatedBy(), checklist.getSubmittedBy(), checklist.getApprovedBy());
    }

    private static ChecklistSummary toSummary(OperationsChecklist checklist, Progress progress, Names names) {
        MaintenanceAircraft aircraft = checklist.getAircraft();
        return new ChecklistSummary(checklist.getId(), aircraft.getId(), aircraft.getRegistration(), aircraft.getType(), checklist.getKind(),
                checklist.getDate(), checklist.getStatus(), names.of(checklist.getCreatedBy()), checklist.getCreatedAt(),
                names.of(checklist.getSubmittedBy()), checklist.getSubmittedAt(), names.of(checklist.getApprovedBy()), checklist.getApprovedAt(),
                progress == null ? 0 : progress.getTotal(), progress == null ? 0 : progress.getChecked(), progress == null ? 0 : progress.getNotOk());
    }

    private ChecklistDetail toDetail(OperationsChecklist checklist, UserDetailsImpl currentUser) {
        List<OperationsChecklistItem> items = itemRepository.findAllByChecklistOrderByPosition(checklist);
        Names names = access.names(Stream.concat(people(checklist), items.stream().map(OperationsChecklistItem::getCheckedBy)).toList());
        long checked = items.stream().filter(item -> item.getResult() != null).count();
        long notOk = items.stream().filter(item -> item.getResult() == ChecklistItemResult.NOT_OK).count();
        ChecklistSummary summary = new ChecklistSummary(checklist.getId(), checklist.getAircraft().getId(), checklist.getAircraft().getRegistration(),
                checklist.getAircraft().getType(), checklist.getKind(), checklist.getDate(), checklist.getStatus(), names.of(checklist.getCreatedBy()),
                checklist.getCreatedAt(), names.of(checklist.getSubmittedBy()), checklist.getSubmittedAt(), names.of(checklist.getApprovedBy()),
                checklist.getApprovedAt(), items.size(), checked, notOk);
        boolean editable = isEditable(checklist);
        return new ChecklistDetail(summary,
                items.stream().map(item -> new ItemResponse(item.getId(), item.getPosition(), item.getText(), item.getResult(), item.getComment(),
                        names.of(item.getCheckedBy()), item.getCheckedAt(),
                        item.getDefect() == null ? null : new DefectRef(item.getDefect().getId(), item.getDefect().getTitle(), item.getDefect().getStatus())))
                        .toList(),
                editable, editable && checked == items.size(),
                checklist.getStatus() == ChecklistStatus.SUBMITTED && access.canManageOperations(currentUser),
                isReopenable(checklist, currentUser));
    }
}
