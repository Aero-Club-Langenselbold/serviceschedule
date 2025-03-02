package de.ct.manager.schedule.service;

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
@RequestMapping("/service")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Service>> listServices() {
        return ResponseEntity.ok(serviceService.listServices());
    }

    @GetMapping("/fetch/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Service> getService(@PathVariable String id) {
        return ResponseEntity.ok(serviceService.getService(id));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Service> createOrUpdateService(@RequestBody Service service) {
        return ResponseEntity.ok(serviceService.createOrUpdateService(service));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        serviceService.deleteService(id);
        return ResponseEntity.ok().build();
    }
}
