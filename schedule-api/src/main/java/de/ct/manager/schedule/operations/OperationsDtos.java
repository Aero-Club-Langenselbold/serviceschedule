package de.ct.manager.schedule.operations;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.DefectRef;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.Person;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Request and response bodies of the operations checklists. Entities never leave the service layer. */
public final class OperationsDtos {

    private OperationsDtos() {
    }

    // --- templates

    /** Blank items are left out. */
    public record TemplateRequest(@NotEmpty @Size(max = 200) List<@NotNull @Size(max = 500) String> items) {
    }

    public record TemplateResponse(String id, String aircraftId, ChecklistKind kind, List<String> items, Instant updatedAt) {
    }

    // --- checklists

    /** progress: items checked (OK or NOT_OK) of itemCount, notOkCount of them NOT_OK. */
    public record ChecklistSummary(
            String id,
            String aircraftId,
            String registration,
            String aircraftType,
            ChecklistKind kind,
            LocalDate date,
            ChecklistStatus status,
            /** The initial recorder, who started the checklist. */
            Person createdBy,
            Instant createdAt,
            Person submittedBy,
            Instant submittedAt,
            Person approvedBy,
            Instant approvedAt,
            long itemCount,
            long checkedCount,
            long notOkCount) {
    }

    public record ItemResponse(String id, int position, String text, ChecklistItemResult result, String comment, Person checkedBy,
            Instant checkedAt, DefectRef defect) {
    }

    /** The flags tell the UI what the caller may do, so it does not duplicate the rules. */
    public record ChecklistDetail(
            ChecklistSummary checklist,
            List<ItemResponse> items,
            boolean editable,
            boolean submittable,
            boolean approvable,
            boolean reopenable) {
    }

    /** result null unchecks the item. */
    public record ItemRequest(ChecklistItemResult result, @Size(max = 1000) String comment) {
    }

    /**
     * What the fixed link (QR code) of an aircraft and kind opens: the checklist of today, or (checklist null) the
     * template items to start it with, templateItems null if the aircraft has no such checklist.
     */
    public record TodayResponse(
            String aircraftId,
            String registration,
            String aircraftType,
            ChecklistKind kind,
            LocalDate date,
            List<String> templateItems,
            ChecklistDetail checklist) {
    }

    /** An aircraft with its checklist templates and the checklists of today, for the overview page. */
    public record AircraftChecklists(
            String aircraftId,
            String registration,
            String aircraftType,
            TemplateResponse startTemplate,
            TemplateResponse endTemplate,
            ChecklistSummary todayStart,
            ChecklistSummary todayEnd) {
    }

    public record OverviewResponse(LocalDate today, List<AircraftChecklists> aircraft) {
    }
}
