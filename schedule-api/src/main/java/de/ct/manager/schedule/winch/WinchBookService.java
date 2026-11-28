package de.ct.manager.schedule.winch;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceAccess;
import de.ct.manager.schedule.maintenance.MaintenanceAccess.Names;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.winch.WinchDtos.DayDetail;
import de.ct.manager.schedule.winch.WinchDtos.DaySummary;
import de.ct.manager.schedule.winch.WinchDtos.IncidentRequest;
import de.ct.manager.schedule.winch.WinchDtos.IncidentResponse;
import de.ct.manager.schedule.winch.WinchDtos.OptionRef;
import de.ct.manager.schedule.winch.WinchDtos.OptionResponse;
import de.ct.manager.schedule.winch.WinchDtos.PhotoResponse;
import de.ct.manager.schedule.winch.WinchDtos.RemarkRequest;
import de.ct.manager.schedule.winch.WinchDtos.RemarkResponse;
import de.ct.manager.schedule.winch.WinchDtos.RopeEntries;
import de.ct.manager.schedule.winch.WinchDtos.SettingsResponse;
import de.ct.manager.schedule.winch.WinchDtos.TypeCount;
import lombok.extern.slf4j.Slf4j;

/**
 * The winch book ("Windenbuch") of the club's winch.
 * <ul>
 * <li>Maintenance administrators define the ropes of the winch and the kinds of incidents. Options referenced by
 * entries are only deactivated when removed, so the book keeps them.</li>
 * <li>Every signed in member (also in the kiosk mode) writes into the page of today: incidents per rope (any number,
 * the caller is recorded as winch driver), photos per rope, the general remark of the day and a remark per rope.
 * Whoever writes into the page of today becomes a winch driver of the day. Members delete their own incidents and
 * photos of today.</li>
 * <li>Everybody reads the pages of earlier days. Only maintenance administrators change them: they correct remarks and
 * delete incidents and photos, but nobody adds incidents or photos to an earlier day.</li>
 * </ul>
 * A page without any entry left is removed.
 */
@Service
@Slf4j
public class WinchBookService {

    /** Largest photo accepted (10 MB); the UI scales photos down before uploading them. */
    public static final int MAX_PHOTO_BYTES = 10 * 1024 * 1024;

    static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final WinchRopeRepository ropeRepository;
    private final WinchIncidentTypeRepository typeRepository;
    private final WinchDayRepository dayRepository;
    private final WinchRopeDayRepository ropeDayRepository;
    private final WinchIncidentRepository incidentRepository;
    private final WinchPhotoRepository photoRepository;
    private final WinchPhotoContentRepository contentRepository;
    private final MaintenanceAccess access;
    private final ZoneId zone;

    public WinchBookService(WinchRopeRepository ropeRepository, WinchIncidentTypeRepository typeRepository, WinchDayRepository dayRepository,
            WinchRopeDayRepository ropeDayRepository, WinchIncidentRepository incidentRepository, WinchPhotoRepository photoRepository,
            WinchPhotoContentRepository contentRepository, MaintenanceAccess access,
            @Value("${schedule.calendar.timezone:Europe/Berlin}") String zone) {
        this.ropeRepository = ropeRepository;
        this.typeRepository = typeRepository;
        this.dayRepository = dayRepository;
        this.ropeDayRepository = ropeDayRepository;
        this.incidentRepository = incidentRepository;
        this.photoRepository = photoRepository;
        this.contentRepository = contentRepository;
        this.access = access;
        this.zone = ZoneId.of(zone);
    }

    /** The day of the club, independent of the time zone of the server. */
    public LocalDate today() {
        return LocalDate.now(zone);
    }

    // --- settings

    /** The repository and rules of one list of options; ropes and incident types are handled alike. */
    private record Options<T extends WinchOption>(JpaRepository<T, String> repository, Supplier<List<T>> all, Supplier<T> create,
            Predicate<T> used, Supplier<Set<String>> usedIds, String what) {
    }

    private Options<? extends WinchOption> options(WinchOptionKind kind) {
        return switch (kind) {
            case ROPES -> new Options<>(ropeRepository, ropeRepository::findAllByOrderByPositionAscNameAsc, WinchRope::new,
                    rope -> incidentRepository.existsByRope(rope) || photoRepository.existsByRope(rope) || ropeDayRepository.existsByRope(rope),
                    () -> Stream.of(incidentRepository.findUsedRopeIds(), photoRepository.findUsedRopeIds(), ropeDayRepository.findUsedRopeIds())
                            .flatMap(List::stream).collect(Collectors.toSet()),
                    "Rope");
            case INCIDENT_TYPES -> new Options<>(typeRepository, typeRepository::findAllByOrderByPositionAscNameAsc, WinchIncidentType::new,
                    incidentRepository::existsByType, () -> new HashSet<>(incidentRepository.findUsedTypeIds()), "Incident type");
        };
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        return new SettingsResponse(toOptions(options(WinchOptionKind.ROPES)), toOptions(options(WinchOptionKind.INCIDENT_TYPES)));
    }

    private static <T extends WinchOption> List<OptionResponse> toOptions(Options<T> options) {
        Set<String> used = options.usedIds().get();
        return options.all().get().stream()
                .map(option -> new OptionResponse(option.getId(), option.getName(), option.getPosition(), option.isActive(), used.contains(option.getId())))
                .toList();
    }

    @Transactional
    public SettingsResponse createOption(WinchOptionKind kind, String name, UserDetailsImpl currentUser) {
        access.requireManage(currentUser);
        createOption(options(kind), name);
        log.info("User {} added {} '{}' to the winch book", currentUser.getUsername(), kind, name.strip());
        return getSettings();
    }

    private static <T extends WinchOption> void createOption(Options<T> options, String name) {
        List<T> all = options.all().get();
        String stripped = requireUniqueName(all, name, null);
        T option = options.create().get();
        option.setName(stripped);
        option.setPosition(all.stream().mapToInt(WinchOption::getPosition).max().orElse(-1) + 1);
        options.repository().save(option);
    }

    /** Renames, deactivates or reactivates an option. */
    @Transactional
    public SettingsResponse updateOption(WinchOptionKind kind, String id, String name, boolean active, UserDetailsImpl currentUser) {
        access.requireManage(currentUser);
        updateOption(options(kind), id, name, active);
        return getSettings();
    }

    private static <T extends WinchOption> void updateOption(Options<T> options, String id, String name, boolean active) {
        T option = options.repository().findById(id).orElseThrow(() -> MaintenanceAccess.notFound(options.what()));
        option.setName(requireUniqueName(options.all().get(), name, id));
        option.setActive(active);
        options.repository().save(option);
    }

    /** Deletes an option nothing refers to; one referenced by entries is deactivated instead and stays in the book. */
    @Transactional
    public SettingsResponse removeOption(WinchOptionKind kind, String id, UserDetailsImpl currentUser) {
        access.requireManage(currentUser);
        boolean deleted = removeOption(options(kind), id);
        log.info("User {} {} the {} {} of the winch book", currentUser.getUsername(), deleted ? "deleted" : "deactivated", kind, id);
        return getSettings();
    }

    private static <T extends WinchOption> boolean removeOption(Options<T> options, String id) {
        T option = options.repository().findById(id).orElseThrow(() -> MaintenanceAccess.notFound(options.what()));
        if (options.used().test(option)) {
            option.setActive(false);
            options.repository().save(option);
            return false;
        }
        options.repository().delete(option);
        return true;
    }

    /** Puts the options in the given order; ids has to name every option of the list exactly once. */
    @Transactional
    public SettingsResponse reorder(WinchOptionKind kind, List<String> ids, UserDetailsImpl currentUser) {
        access.requireManage(currentUser);
        reorder(options(kind), ids);
        return getSettings();
    }

    private static <T extends WinchOption> void reorder(Options<T> options, List<String> ids) {
        Map<String, T> all = options.all().get().stream().collect(Collectors.toMap(WinchOption::getId, Function.identity()));
        if (ids.size() != all.size() || !all.keySet().equals(new HashSet<>(ids))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The order has to contain every entry exactly once");
        }
        for (int position = 0; position < ids.size(); position++) {
            all.get(ids.get(position)).setPosition(position);
        }
        options.repository().saveAll(all.values());
    }

    private static String requireUniqueName(List<? extends WinchOption> all, String name, String ownId) {
        String stripped = name == null ? "" : name.strip();
        if (stripped.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A name is required");
        }
        if (all.stream().anyMatch(option -> !option.getId().equals(ownId) && option.getName().equalsIgnoreCase(stripped))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The name is already used");
        }
        return stripped;
    }

    // --- the page of a day

    @Transactional(readOnly = true)
    public DayDetail getDay(LocalDate date, UserDetailsImpl currentUser) {
        Optional<WinchDay> day = dayRepository.findByDate(date);
        if (day.isEmpty() && !date.equals(today())) {
            throw MaintenanceAccess.notFound("Winch book entry");
        }
        return toDetail(date, day, currentUser);
    }

    /** Records an incident of a rope for today, with the caller as winch driver. */
    @Transactional
    public DayDetail addIncident(IncidentRequest request, UserDetailsImpl currentUser) {
        WinchRope rope = findRope(request.ropeId());
        WinchIncidentType type = typeRepository.findById(request.typeId()).orElseThrow(() -> MaintenanceAccess.notFound("Incident type"));
        requireActive(rope);
        requireActive(type);
        User driver = access.currentUser(currentUser);
        WinchDay day = todayForWriting(driver);
        WinchIncident incident = new WinchIncident();
        incident.setDay(day);
        incident.setRope(rope);
        incident.setType(type);
        incident.setDriver(driver);
        incident.setComment(blankToNull(request.comment()));
        incidentRepository.save(incident);
        log.info("User {} recorded '{}' on '{}' in the winch book", currentUser.getUsername(), type.getName(), rope.getName());
        return toDetail(day.getDate(), Optional.of(day), currentUser);
    }

    @Transactional
    public DayDetail deleteIncident(String id, UserDetailsImpl currentUser) {
        WinchIncident incident = incidentRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Incident"));
        WinchDay day = incident.getDay();
        if (!isDeletable(day, incident.getDriver(), currentUser)) {
            throw new AccessDeniedException("Only own entries of today, or maintenance administrators");
        }
        incidentRepository.delete(incident);
        incidentRepository.flush();
        log.info("User {} deleted the incident {} of {} from the winch book", currentUser.getUsername(), id, day.getDate());
        return afterRemoval(day, currentUser);
    }

    /** The general remark of a day; blank removes it. */
    @Transactional
    public DayDetail saveDayRemark(LocalDate date, RemarkRequest request, UserDetailsImpl currentUser) {
        String text = blankToNull(request.text());
        Optional<WinchDay> existing = dayForRemark(date, currentUser);
        if (existing.isPresent()) {
            requireCurrent(existing.get().getRemark(), existing.get().getVersion(), request.version());
        } else if (text == null) {
            return toDetail(date, existing, currentUser);
        }
        User actor = access.currentUser(currentUser);
        WinchDay day = existing.orElseGet(() -> todayForWriting(actor));
        if (date.equals(today())) {
            day.getDrivers().add(actor);
        }
        if (!Objects.equals(day.getRemark(), text)) {
            day.setRemark(text);
            day.setRemarkUpdatedBy(text == null ? null : actor);
            day.setRemarkUpdatedAt(text == null ? null : Instant.now());
        }
        day = dayRepository.saveAndFlush(day);
        return afterRemoval(day, currentUser);
    }

    /** The remark of a rope on a day; blank removes it. */
    @Transactional
    public DayDetail saveRopeRemark(LocalDate date, String ropeId, RemarkRequest request, UserDetailsImpl currentUser) {
        WinchRope rope = findRope(ropeId);
        String text = blankToNull(request.text());
        Optional<WinchDay> existing = dayForRemark(date, currentUser);
        Optional<WinchRopeDay> ropeDay = existing.flatMap(day -> ropeDayRepository.findByDayAndRope(day, rope));
        if (ropeDay.isPresent()) {
            requireCurrent(ropeDay.get().getRemark(), ropeDay.get().getVersion(), request.version());
        } else if (text == null) {
            return toDetail(date, existing, currentUser);
        }
        if (ropeDay.isEmpty() && !rope.isActive() && !date.equals(today())) {
            // an earlier day only shows the ropes it has entries of
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The rope has no entries on that day");
        }
        User actor = access.currentUser(currentUser);
        WinchDay day = existing.orElseGet(() -> todayForWriting(actor));
        if (date.equals(today())) {
            day.getDrivers().add(actor);
        }
        if (text == null) {
            ropeDayRepository.delete(ropeDay.get());
            ropeDayRepository.flush();
        } else {
            WinchRopeDay remark = ropeDay.orElseGet(() -> {
                WinchRopeDay created = new WinchRopeDay();
                created.setDay(day);
                created.setRope(rope);
                return created;
            });
            if (!text.equals(remark.getRemark())) {
                remark.setRemark(text);
                remark.setUpdatedBy(actor);
                remark.setUpdatedAt(Instant.now());
            }
            ropeDayRepository.saveAndFlush(remark);
        }
        return afterRemoval(dayRepository.save(day), currentUser);
    }

    /** Adds a photo of a rope to the page of today. */
    @Transactional
    public DayDetail addPhoto(String ropeId, MultipartFile file, UserDetailsImpl currentUser) {
        WinchRope rope = findRope(ropeId);
        requireActive(rope);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The photo is empty");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!PHOTO_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WebP and GIF images can be uploaded");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "The photo is larger than 10 MB");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The photo could not be read");
        }
        User uploader = access.currentUser(currentUser);
        WinchDay day = todayForWriting(uploader);
        WinchPhoto photo = new WinchPhoto();
        photo.setDay(day);
        photo.setRope(rope);
        photo.setUploadedBy(uploader);
        photo.setFilename(filename(file.getOriginalFilename()));
        photo.setContentType(contentType);
        photo.setSize(data.length);
        photo = photoRepository.save(photo);
        WinchPhotoContent content = new WinchPhotoContent();
        content.setPhoto(photo);
        content.setData(data);
        contentRepository.save(content);
        log.info("User {} added a photo of '{}' to the winch book", currentUser.getUsername(), rope.getName());
        return toDetail(day.getDate(), Optional.of(day), currentUser);
    }

    /** The image of a photo with its content type. */
    public record PhotoData(String contentType, String filename, byte[] data) {
    }

    @Transactional(readOnly = true)
    public PhotoData getPhoto(String id) {
        WinchPhoto photo = photoRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Photo"));
        WinchPhotoContent content = contentRepository.findByPhoto(photo).orElseThrow(() -> MaintenanceAccess.notFound("Photo"));
        return new PhotoData(photo.getContentType(), photo.getFilename(), content.getData());
    }

    @Transactional
    public DayDetail deletePhoto(String id, UserDetailsImpl currentUser) {
        WinchPhoto photo = photoRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Photo"));
        WinchDay day = photo.getDay();
        if (!isDeletable(day, photo.getUploadedBy(), currentUser)) {
            throw new AccessDeniedException("Only own entries of today, or maintenance administrators");
        }
        photoRepository.delete(photo);
        photoRepository.flush();
        log.info("User {} deleted the photo {} of {} from the winch book", currentUser.getUsername(), id, day.getDate());
        return afterRemoval(day, currentUser);
    }

    // --- list of days

    /** The pages of a year, newest first. */
    @Transactional(readOnly = true)
    public Page<DaySummary> listDays(int year, Pageable pageable) {
        Page<WinchDay> page = dayRepository.findAllByDateBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), pageable);
        List<WinchDay> days = page.getContent();
        if (days.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }
        Map<String, Set<User>> drivers = dayRepository.findAllWithDriversByIdIn(days.stream().map(WinchDay::getId).toList()).stream()
                .collect(Collectors.toMap(WinchDay::getId, WinchDay::getDrivers));
        Map<String, List<WinchIncidentRepository.TypeCount>> counts = incidentRepository.countByDayAndType(days).stream()
                .collect(Collectors.groupingBy(WinchIncidentRepository.TypeCount::getDayId));
        Map<String, Long> photos = photoRepository.countByDay(days).stream()
                .collect(Collectors.toMap(WinchPhotoRepository.DayCount::getDayId, WinchPhotoRepository.DayCount::getTotal));
        Set<String> ropeRemarks = new HashSet<>(ropeDayRepository.findDayIdsWithRemarks(days));
        Names names = access.names(drivers.values().stream().flatMap(Set::stream).toList());
        return page.map(day -> {
            List<TypeCount> incidents = counts.getOrDefault(day.getId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(WinchIncidentRepository.TypeCount::getSortOrder).thenComparing(WinchIncidentRepository.TypeCount::getTypeName))
                    .map(count -> new TypeCount(count.getTypeId(), count.getTypeName(), count.getTotal()))
                    .toList();
            return new DaySummary(day.getDate(), sortedPeople(drivers.getOrDefault(day.getId(), Set.of()), names), incidents,
                    incidents.stream().mapToLong(TypeCount::count).sum(), photos.getOrDefault(day.getId(), 0L),
                    day.getRemark() != null || ropeRemarks.contains(day.getId()));
        });
    }

    /** The years with entries and the current one, newest first. */
    @Transactional(readOnly = true)
    public List<Integer> years() {
        Set<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        years.addAll(dayRepository.findYears());
        years.add(today().getYear());
        return List.copyOf(years);
    }

    // --- helpers

    private WinchRope findRope(String id) {
        return ropeRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Rope"));
    }

    private static void requireActive(WinchOption option) {
        if (!option.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'" + option.getName() + "' is no longer in use");
        }
    }

    /** The page of today, created with its first entry; the writer becomes a winch driver of the day. */
    private WinchDay todayForWriting(User writer) {
        LocalDate today = today();
        WinchDay day = dayRepository.findByDate(today).orElseGet(() -> {
            WinchDay created = new WinchDay();
            created.setDate(today);
            return created;
        });
        day.getDrivers().add(writer);
        try {
            return dayRepository.saveAndFlush(day);
        } catch (DataIntegrityViolationException e) {
            // somebody else started the page of today at the same moment
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The winch book was changed at the same time, please try again");
        }
    }

    /** Remarks of today are written by everybody, those of earlier days only corrected by maintenance administrators. */
    private Optional<WinchDay> dayForRemark(LocalDate date, UserDetailsImpl currentUser) {
        LocalDate today = today();
        if (date.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only today can be written");
        }
        Optional<WinchDay> day = dayRepository.findByDate(date);
        if (!date.equals(today)) {
            access.requireManage(currentUser);
            if (day.isEmpty()) {
                throw MaintenanceAccess.notFound("Winch book entry");
            }
        }
        return day;
    }

    /**
     * Somebody else changed the remark since the caller loaded it (version is the one the caller saw, null if there was
     * none). There is only something to overwrite if the remark has a text.
     */
    private static void requireCurrent(String remark, Integer currentVersion, Integer loadedVersion) {
        if (remark != null && !Objects.equals(currentVersion, loadedVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The remark was changed by somebody else in the meantime");
        }
    }

    private boolean isDeletable(WinchDay day, User author, UserDetailsImpl currentUser) {
        return access.canManage(currentUser) || (day.getDate().equals(today()) && MaintenanceAccess.isSameUser(author, currentUser));
    }

    /** Removes the page of a day once nothing is left on it and answers the (possibly empty) page. */
    private DayDetail afterRemoval(WinchDay day, UserDetailsImpl currentUser) {
        if (day.getRemark() == null && !incidentRepository.existsByDay(day) && !photoRepository.existsByDay(day) && !ropeDayRepository.existsByDay(day)) {
            dayRepository.delete(day);
            dayRepository.flush();
            return toDetail(day.getDate(), Optional.empty(), currentUser);
        }
        return toDetail(day.getDate(), Optional.of(day), currentUser);
    }

    private DayDetail toDetail(LocalDate date, Optional<WinchDay> day, UserDetailsImpl currentUser) {
        boolean isToday = date.equals(today());
        boolean canManage = access.canManage(currentUser);
        List<WinchIncident> incidents = day.map(incidentRepository::findAllByDayOrderByCreatedAt).orElse(List.of());
        List<WinchPhoto> photos = day.map(photoRepository::findAllByDayOrderByUploadedAt).orElse(List.of());
        List<WinchRopeDay> remarks = day.map(ropeDayRepository::findAllByDay).orElse(List.of());

        List<User> people = new ArrayList<>();
        day.ifPresent(found -> {
            people.add(found.getRemarkUpdatedBy());
            people.addAll(found.getDrivers());
        });
        incidents.forEach(incident -> people.add(incident.getDriver()));
        photos.forEach(photo -> people.add(photo.getUploadedBy()));
        remarks.forEach(remark -> people.add(remark.getUpdatedBy()));
        Names names = access.names(people);

        // today every rope in use, plus those with entries; an earlier day only the ropes it has entries of
        Map<String, WinchRope> ropes = new LinkedHashMap<>();
        Stream.concat(isToday ? ropeRepository.findAllByOrderByPositionAscNameAsc().stream().filter(WinchOption::isActive) : Stream.empty(),
                Stream.of(incidents.stream().map(WinchIncident::getRope), photos.stream().map(WinchPhoto::getRope), remarks.stream().map(WinchRopeDay::getRope))
                        .flatMap(Function.identity()))
                .forEach(rope -> ropes.putIfAbsent(rope.getId(), rope));
        List<RopeEntries> ropeEntries = ropes.values().stream()
                .sorted(Comparator.comparingInt(WinchRope::getPosition).thenComparing(WinchRope::getName))
                .map(rope -> new RopeEntries(ref(rope),
                        remarks.stream().filter(remark -> remark.getRope().getId().equals(rope.getId())).findFirst()
                                .map(remark -> new RemarkResponse(remark.getRemark(), remark.getVersion(), names.of(remark.getUpdatedBy()), remark.getUpdatedAt()))
                                .orElse(new RemarkResponse(null, null, null, null)),
                        incidents.stream().filter(incident -> incident.getRope().getId().equals(rope.getId()))
                                .map(incident -> new IncidentResponse(incident.getId(), ref(incident.getType()), names.of(incident.getDriver()),
                                        incident.getCreatedAt(), incident.getComment(), canManage || (isToday && MaintenanceAccess.isSameUser(incident.getDriver(), currentUser))))
                                .toList(),
                        photos.stream().filter(photo -> photo.getRope().getId().equals(rope.getId()))
                                .map(photo -> new PhotoResponse(photo.getId(), photo.getFilename(), photo.getContentType(), photo.getSize(),
                                        names.of(photo.getUploadedBy()), photo.getUploadedAt(),
                                        canManage || (isToday && MaintenanceAccess.isSameUser(photo.getUploadedBy(), currentUser))))
                                .toList()))
                .toList();

        RemarkResponse remark = day.map(found -> new RemarkResponse(found.getRemark(), found.getVersion(), names.of(found.getRemarkUpdatedBy()),
                found.getRemarkUpdatedAt())).orElse(new RemarkResponse(null, null, null, null));
        List<OptionRef> types = isToday
                ? typeRepository.findAllByOrderByPositionAscNameAsc().stream().filter(WinchOption::isActive).map(WinchBookService::ref).toList()
                : List.of();
        return new DayDetail(date, isToday, day.isPresent(), remark, sortedPeople(day.map(WinchDay::getDrivers).orElse(Set.of()), names),
                ropeEntries, types, isToday, isToday || (canManage && day.isPresent()));
    }

    private static List<Person> sortedPeople(Set<User> users, Names names) {
        return users.stream().map(names::of).sorted(Comparator.comparing(Person::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private static OptionRef ref(WinchOption option) {
        return new OptionRef(option.getId(), option.getName(), option.isActive());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** Only the name of the file, without any path a browser might send. */
    private static String filename(String original) {
        if (original == null || original.isBlank()) {
            return null;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        return name.isEmpty() ? null : name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
