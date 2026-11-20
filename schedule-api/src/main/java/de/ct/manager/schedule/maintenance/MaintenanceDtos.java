package de.ct.manager.schedule.maintenance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
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
            /** The current workshop period or else the next one, null if none is planned. */
            WorkshopSlotResponse workshop,
            long taskCount,
            long doneCount,
            long signedCount,
            /** Checklist steps of all tasks (a task without checklist is one step) – drives the progress in the schedule. */
            long stepCount,
            long stepDoneCount,
            long openDefectCount) {
    }

    // --- workshop schedule

    public record WorkshopSlotRequest(
            @NotBlank @Size(max = 36) String aircraftId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Size(max = 255) String note) {
    }

    public record WorkshopSlotResponse(String id, String aircraftId, LocalDate startDate, LocalDate endDate, String note) {
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
            List<@NotBlank @Size(max = 500) String> checklist) {
    }

    public record DefectRef(String id, String title, DefectStatus status) {
    }

    /** Card on the kanban board. */
    public record TaskSummary(
            String id,
            String aircraftId,
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

    // --- defects

    public record DefectRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @NotNull DefectPriority priority) {
    }

    public record DefectStatusRequest(@NotNull DefectStatus status) {
    }

    public record TaskRef(String id, String title, TaskStatus status) {
    }

    public record DefectResponse(
            String id,
            String aircraftId,
            String title,
            String description,
            DefectPriority priority,
            DefectStatus status,
            Person reportedBy,
            Instant reportedAt,
            Person doneBy,
            Instant doneAt,
            TaskRef task,
            boolean editable) {
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
