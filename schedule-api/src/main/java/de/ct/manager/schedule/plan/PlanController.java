package de.ct.manager.schedule.plan;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.participant.DateParticipantAvailability;
import de.ct.manager.schedule.service.ServiceDate;



@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Plan>> listPlans() {
        return ResponseEntity.ok(planService.listPlans());
    }

    @GetMapping("/fetch/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Plan> getPlan(@PathVariable String id) {
        return ResponseEntity.ok(planService.getPlan(id));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Plan> createOrUpdatePlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(planService.createOrUpdatePlan(plan));
    }

    @PostMapping("/generate/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Plan> generatePlan(@PathVariable String id) {
        return ResponseEntity.ok(planService.generatePlan(id));
    }

    @PostMapping("/updatePlanEntry/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PlanEntry>> updatePlanEntry(@PathVariable String id, @RequestBody List<PlanEntry> planEntry) {
        return ResponseEntity.ok(planService.updatePlanEntry(id, planEntry));
    }

    @GetMapping("/download/pdf/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadPlanAsPdf(@PathVariable String id) {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=plan_" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(planService.generatePlanPDF(id));
    }

    @GetMapping("/download/csv/{planId}/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadPlanAsCsv(@PathVariable String planId, @PathVariable String serviceId) {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=plan_" + planId + "_" + serviceId + ".csv")
            .contentType(MediaType.TEXT_PLAIN)
            .body(planService.generatePlanCSV(planId, serviceId));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlan(@PathVariable String id) {
        planService.deletePlan(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/serviceDates/save/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceDate>> createOrUpdateServiceDate(@PathVariable String id, @RequestBody List<ServiceDate> serviceDates) {
        return ResponseEntity.ok(planService.addServiceDates(id, serviceDates));
    }

    @DeleteMapping("/serviceDates/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteServiceDate(@PathVariable String id) {
        planService.deleteServiceDate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/participantAvailability/save/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> createOrUpdateParticipantAvailability(@PathVariable String id, @RequestBody List<DateParticipantAvailability> participantAvailability) {
        planService.addDateParticipantAvailability(id, participantAvailability);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/participantAvailability/fetch/{planId}/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<DateParticipantAvailability>> getParticipantAvailability(@PathVariable String planId, @PathVariable String userId) {
        return ResponseEntity.ok(planService.getDateParticipantAvailability(planId, userId));
    }

    @GetMapping("/participantAvailability/fetch/{planId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<DateParticipantAvailability>> getAllParticipantAvailability(@PathVariable String planId) {
        return ResponseEntity.ok(planService.getDateParticipantAvailability(planId));
    }

    @GetMapping("/participantAvailability/fetch/{planId}/{serviceId}/{date}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<DateParticipantAvailability>> getAllParticipantAvailability(@PathVariable String planId, @PathVariable String serviceId, @PathVariable String date) {
        return ResponseEntity.ok(planService.getDateParticipantAvailability(planId, date, serviceId));
    }

    @GetMapping("/statistics/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PlanStatistics>> getPlanStatistics(@PathVariable String planId) {
        return ResponseEntity.ok(planService.getPlanStatistics(planId));
    }

}
