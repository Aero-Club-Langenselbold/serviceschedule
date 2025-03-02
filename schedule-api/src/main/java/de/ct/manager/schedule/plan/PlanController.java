package de.ct.manager.schedule.plan;

import java.util.List;

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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Plan>> listPlans() {
        return ResponseEntity.ok(planService.listPlans());
    }

    @GetMapping("/fetch/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Plan> getPlan(@PathVariable String id) {
        return ResponseEntity.ok(planService.getPlan(id));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Plan> createOrUpdatePlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(planService.createOrUpdatePlan(plan));
    }

    @PostMapping("/generate/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Plan> generatePlan(@PathVariable String id, @RequestBody PlanDetails planDetails) {
        return ResponseEntity.ok(planService.generatePlan(id, planDetails.getServiceDates(), planDetails.getAvailability()));
    }


    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deletePlan(@PathVariable String id) {
        planService.deletePlan(id);
        return ResponseEntity.ok().build();
    }
}
