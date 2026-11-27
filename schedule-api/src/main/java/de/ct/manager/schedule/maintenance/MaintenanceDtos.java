package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request and response bodies of the maintenance API. Entities never leave the service layer. */
public final class MaintenanceDtos {

    private MaintenanceDtos() {
    }

    /** A user as shown in the UI: participant name if known, otherwise the username. */
    public record Person(String id, String name) {
    }

    // --- aircraft

    public record AircraftRequest(
            @NotBlank @Size(max = 100) String type,
            @NotBlank @Size(max = 20) String registration) {
    }

    public record AircraftResponse(
            String id,
            String type,
            String registration,
            /** The current workshop period or else the next one with its progress, null if none is planned. */
            WorkshopSlotResponse workshop,
            /** Tasks on the board without workshop period. */
            long unscheduledTaskCount,
            long openDefectCount) {
    }

    // --- workshop schedule

    public record WorkshopSlotRequest(
            @NotBlank @Size(max = 36) String aircraftId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Size(max = 255) String note) {
    }

    /** A workshop visit with the progress of its board. */
    public record WorkshopSlotResponse(
            String id,
            String aircraftId,
            LocalDate startDate,
            LocalDate endDate,
            String note,
            long taskCount,
            long doneCount,
            long signedCount,
            /** Checklist steps of the tasks of the visit (a task without checklist is one step). */
            long stepCount,
            long stepDoneCount) {
    }

    /** Creates a task from each of the chosen templates on the board of the workshop visit (null = without period). */
    public record TemplateImportRequest(
            @Size(max = 36) String workshopId,
            @NotEmpty List<@NotBlank @Size(max = 36) String> templateIds) {
    }

    public record ImportResponse(int created) {
    }

    // --- templates

    public record TemplateRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @NotNull SkillLevel level,
            @Positive @Max(10000) Integer durationMinutes,
            Set<@NotBlank @Size(max = 100) String> aircraftTypes,
            List<@NotBlank @Size(max = 500) String> checklist) {
    }

    public record TemplateResponse(
            String id,
            String title,
            String description,
            SkillLevel level,
            Integer durationMinutes,
            List<String> aircraftTypes,
            List<String> checklist) {
    }

    // --- tasks

    public record TaskRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @NotNull SkillLevel level,
            @Positive @Max(10000) Integer durationMinutes,
            LocalDate deadline,
            String defectId,
            /** Workshop visit of the same aircraft, null for the board without period. */
            @Size(max = 36) String workshopId,
            List<@NotBlank @Size(max = 500) String> checklist) {
    }

    public record DefectRef(String id, String title, DefectStatus status) {
    }

    /** Card on the kanban board. */
    public record TaskSummary(
            String id,
            String aircraftId,
            String workshopId,
            String templateId,
            String title,
            String description,
            SkillLevel level,
            TaskStatus status,
            Integer durationMinutes,
            LocalDate deadline,
            boolean fromTemplate,
            DefectRef defect,
            long checklistTotal,
            long checklistDone,
            long noteCount,
            Person doneBy,
            Instant doneAt,
            Person signedBy,
            Instant signedAt) {
    }

    public record ChecklistItemResponse(String id, String text, boolean done, Person doneBy, Instant doneAt) {
    }

    public record NoteResponse(String id, String content, Person author, Instant createdAt, boolean deletable) {
    }

    public record HistoryResponse(String id, TaskHistoryAction action, Person actor, Instant createdAt) {
    }

    public record TaskDetail(
            String id,
            String aircraftId,
            String aircraftLabel,
            String workshopId,
            String title,
            String description,
            SkillLevel level,
            TaskStatus status,
            Integer durationMinutes,
            LocalDate deadline,
            boolean fromTemplate,
            DefectRef defect,
            Person doneBy,
            Instant doneAt,
            Person signedBy,
            Instant signedAt,
            List<ChecklistItemResponse> checklist,
            List<NoteResponse> notes,
            List<HistoryResponse> history) {
    }

    public record ChecklistToggleRequest(boolean done) {
    }

    public record TaskStatusRequest(@NotNull TaskStatus status) {
    }

    public record NoteRequest(@NotBlank @Size(max = 5000) String content) {
    }

    // --- defects (defect book)

    /** assigneeIds: null keeps the assignees; only maintenance administrators change them. */
    public record DefectRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @NotNull DefectPriority priority,
            Set<@NotBlank @Size(max = 36) String> assigneeIds) {
    }

    /** comment: optional reason, required for REJECTED. */
    public record DefectStatusRequest(@NotNull DefectStatus status, @Size(max = 5000) String comment) {
    }

    public record DefectCommentRequest(@NotBlank @Size(max = 5000) String content) {
    }

    public record TaskRef(String id, String title, TaskStatus status) {
    }

    public record DefectResponse(
            String id,
            String aircraftId,
            /** Registration and type of the aircraft or equipment, for lists across all of them. */
            String aircraftLabel,
            String title,
            String description,
            DefectPriority priority,
            DefectStatus status,
            Person reportedBy,
            Instant reportedAt,
            /** Who closed the defect (DONE or REJECTED). */
            Person doneBy,
            Instant doneAt,
            Person signedBy,
            Instant signedAt,
            List<Person> assignees,
            boolean assignedToMe,
            long commentCount,
            TaskRef task,
            /** Title, description and priority may be changed by the caller. */
            boolean editable,
            boolean deletable) {
    }

    public record DefectEventResponse(String id, DefectEventAction action, String content, Person actor, Person subject, Instant createdAt,
            boolean deletable) {
    }

    public record DefectDetail(DefectResponse defect, List<DefectEventResponse> events) {
    }

    // --- inventory

    public record InventoryItemRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull InventoryKind kind,
            @Size(max = 255) String location,
            @NotNull InventoryCounting counting,
            @PositiveOrZero @Max(1_000_000) Integer quantity,
            @PositiveOrZero @Max(1_000_000) Integer minQuantity,
            @Size(max = 30) String unit,
            @Size(max = 5000) String comment,
            Set<@NotBlank @Size(max = 100) String> aircraftTypes) {
    }

    public record InventoryQuantityRequest(@NotNull @PositiveOrZero @Max(1_000_000) Integer quantity) {
    }

    public record InventoryItemResponse(
            String id,
            String name,
            InventoryKind kind,
            String location,
            InventoryCounting counting,
            int quantity,
            Integer minQuantity,
            String unit,
            String comment,
            List<String> aircraftTypes,
            InventoryStockStatus status) {
    }

    public record InventoryHistoryResponse(String id, InventoryHistoryAction action, Integer quantity, Person actor, Instant createdAt) {
    }
}
