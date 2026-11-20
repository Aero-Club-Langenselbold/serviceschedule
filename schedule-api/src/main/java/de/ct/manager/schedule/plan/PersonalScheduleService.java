package de.ct.manager.schedule.plan;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import de.ct.manager.schedule.participant.Participant;

/**
 * The services a signed in user is assigned to. Users are matched to participants by e-mail address,
 * only published plans count. Past services disappear together with their plan.
 */
@org.springframework.stereotype.Service
public class PersonalScheduleService {

    /** One assigned service as shown in "my services" and in the calendar export. */
    public record PersonalService(
            String id,
            String planId,
            String planName,
            String serviceName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> colleagues) {
    }

    private static final DateTimeFormatter ICS_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final PlanEntryRepository planEntryRepository;
    private final ZoneId zone;

    public PersonalScheduleService(PlanEntryRepository planEntryRepository,
            @Value("${schedule.calendar.timezone:Europe/Berlin}") String zone) {
        this.planEntryRepository = planEntryRepository;
        this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public List<PersonalService> getServices(String username, boolean includePast) {
        LocalDateTime from = includePast ? LocalDateTime.of(1900, 1, 1, 0, 0) : LocalDateTime.now(zone);
        List<PlanEntry> own = planEntryRepository.findPublishedByParticipantEmail(username, from);
        if (own.isEmpty()) {
            return List.of();
        }

        Set<String> planIds = own.stream().map(entry -> entry.getPlan().getId()).collect(Collectors.toSet());
        Map<String, List<Participant>> onDuty = planEntryRepository.findAssignedByPlanIds(planIds).stream()
            .collect(Collectors.groupingBy(PersonalScheduleService::slot,
                Collectors.mapping(PlanEntry::getParticipant, Collectors.toList())));

        return own.stream()
            .map(entry -> new PersonalService(
                entry.getId(),
                entry.getPlan().getId(),
                entry.getPlan().getName(),
                entry.getService().getServicename(),
                entry.getStartTime(),
                entry.getEndTime(),
                onDuty.getOrDefault(slot(entry), List.of()).stream()
                    .filter(participant -> !username.equalsIgnoreCase(participant.getEmail()))
                    .map(participant -> participant.getFirstname() + " " + participant.getLastname())
                    .distinct()
                    .sorted()
                    .toList()))
            .toList();
    }

    /** An iCalendar file (RFC 5545) with the services, times in UTC so every calendar shows them right. */
    public byte[] toIcs(List<PersonalService> services) {
        String stamp = ICS_UTC.format(Instant.now().atOffset(ZoneOffset.UTC));
        StringBuilder ics = new StringBuilder();
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//Aero-Club Langenselbold//ServiceSchedule//DE");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        for (PersonalService service : services) {
            line(ics, "BEGIN:VEVENT");
            line(ics, "UID:" + service.id() + "@serviceschedule");
            line(ics, "DTSTAMP:" + stamp);
            line(ics, "DTSTART:" + utc(service.startTime()));
            line(ics, "DTEND:" + utc(service.endTime()));
            line(ics, "SUMMARY:" + escape(service.serviceName()));
            String description = service.planName()
                + (service.colleagues().isEmpty() ? "" : "\n" + String.join(", ", service.colleagues()));
            line(ics, "DESCRIPTION:" + escape(description));
            line(ics, "END:VEVENT");
        }
        line(ics, "END:VCALENDAR");
        return ics.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String utc(LocalDateTime time) {
        return ICS_UTC.format(time.atZone(zone).withZoneSameInstant(ZoneOffset.UTC));
    }

    private static String slot(PlanEntry entry) {
        return entry.getPlan().getId() + "|" + Objects.toString(entry.getService() == null ? null : entry.getService().getId()) + "|" + entry.getStartTime();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n");
    }

    /** Appends a content line, folded after 75 octets as the standard requires. */
    private static void line(StringBuilder ics, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        int start = 0;
        int limit = 75;
        while (bytes.length - start > limit) {
            int end = start + limit;
            while ((bytes[end] & 0xC0) == 0x80) { // never split a UTF-8 character
                end--;
            }
            ics.append(new String(bytes, start, end - start, StandardCharsets.UTF_8)).append("\r\n ");
            start = end;
            limit = 74; // the leading space of a continuation line counts
        }
        ics.append(new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8)).append("\r\n");
    }
}
