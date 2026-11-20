package de.ct.manager.schedule.maintenance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.TemplateRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.TemplateResponse;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaintenanceTemplateService {

    private final MaintenanceTemplateRepository templateRepository;

    public MaintenanceTemplateService(MaintenanceTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplates() {
        return templateRepository.findAllByOrderByLevelAscTitleAsc().stream().map(MaintenanceTemplateService::toResponse).toList();
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request, UserDetailsImpl currentUser) {
        MaintenanceTemplate template = new MaintenanceTemplate();
        apply(template, request);
        MaintenanceTemplate saved = templateRepository.save(template);
        log.info("User {} created template {}", currentUser.getUsername(), saved.getTitle());
        return toResponse(saved);
    }

    /** Existing tasks stay as they are, a template only shapes tasks created from it later. */
    @Transactional
    public TemplateResponse update(String id, TemplateRequest request, UserDetailsImpl currentUser) {
        MaintenanceTemplate template = find(id);
        apply(template, request);
        log.info("User {} updated template {}", currentUser.getUsername(), template.getTitle());
        return toResponse(templateRepository.save(template));
    }

    /** Tasks created from the template are kept, they only lose the link. */
    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceTemplate template = find(id);
        templateRepository.delete(template);
        log.info("User {} deleted template {}", currentUser.getUsername(), template.getTitle());
    }

    private MaintenanceTemplate find(String id) {
        return templateRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Template"));
    }

    private static void apply(MaintenanceTemplate template, TemplateRequest request) {
        template.setTitle(request.title().strip());
        template.setDescription(MaintenanceTaskService.blankToNull(request.description()));
        template.setLevel(request.level());
        template.setDurationMinutes(request.durationMinutes());

        Set<String> types = request.aircraftTypes() == null ? Set.of()
                : request.aircraftTypes().stream().map(String::strip).collect(Collectors.toSet());
        template.getAircraftTypes().clear();
        template.getAircraftTypes().addAll(new HashSet<>(types));

        template.getChecklist().clear();
        template.getChecklist().addAll(MaintenanceTaskService.cleanChecklist(request.checklist()));
    }

    private static TemplateResponse toResponse(MaintenanceTemplate template) {
        return new TemplateResponse(template.getId(), template.getTitle(), template.getDescription(), template.getLevel(),
                template.getDurationMinutes(), template.getAircraftTypes().stream().sorted().toList(), new ArrayList<>(template.getChecklist()));
    }
}
