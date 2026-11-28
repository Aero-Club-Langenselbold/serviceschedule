package de.ct.manager.schedule.winch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response bodies of the winch book. Entities never leave the service layer. */
public final class WinchDtos {

    private WinchDtos() {
    }

    // --- settings

    /** used: referenced by recorded entries, so removing it only deactivates it. */
    public record OptionResponse(String id, String name, int position, boolean active, boolean used) {
    }

    public record SettingsResponse(List<OptionResponse> ropes, List<OptionResponse> incidentTypes) {
    }

    public record OptionCreateRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record OptionUpdateRequest(@NotBlank @Size(max = 100) String name, boolean active) {
    }

    /** The ids of all options of the list in their new order. */
    public record OrderRequest(@NotNull List<@NotNull String> ids) {
    }

    // --- a day

    public record OptionRef(String id, String name, boolean active) {
    }

    /** text null if there is no remark; version is sent back when changing it, null while there is none. */
    public record RemarkResponse(String text, Integer version, Person updatedBy, Instant updatedAt) {
    }

    /** version: of the remark as the caller loaded it (null if there was none), a different one answers 409. */
    public record RemarkRequest(@Size(max = 4000) String text, Integer version) {
    }

    public record IncidentRequest(@NotBlank String ropeId, @NotBlank String typeId, @Size(max = 1000) String comment) {
    }

    public record IncidentResponse(String id, OptionRef type, Person driver, Instant createdAt, String comment, boolean deletable) {
    }

    public record PhotoResponse(String id, String filename, String contentType, long size, Person uploadedBy, Instant uploadedAt, boolean deletable) {
    }

    public record RopeEntries(OptionRef rope, RemarkResponse remark, List<IncidentResponse> incidents, List<PhotoResponse> photos) {
    }

    /**
     * The page of a day. exists is false for today until the first entry. editable: incidents and photos can be added
     * (only today); remarksEditable: the remarks can be changed (today everybody, earlier days maintenance administrators).
     */
    public record DayDetail(
            LocalDate date,
            boolean today,
            boolean exists,
            RemarkResponse remark,
            List<Person> drivers,
            List<RopeEntries> ropes,
            List<OptionRef> incidentTypes,
            boolean editable,
            boolean remarksEditable) {
    }

    // --- list of days

    public record TypeCount(String typeId, String name, long count) {
    }

    public record DaySummary(LocalDate date, List<Person> drivers, List<TypeCount> incidents, long incidentCount, long photoCount, boolean hasRemarks) {
    }
}
