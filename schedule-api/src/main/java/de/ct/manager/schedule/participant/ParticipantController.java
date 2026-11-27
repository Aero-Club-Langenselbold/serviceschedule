package de.ct.manager.schedule.participant;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/participant")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERMISSION_SCHEDULE_MANAGE')")
    public ResponseEntity<List<Participant>> listParticipants() {
        return ResponseEntity.ok(participantService.listParticipants());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERMISSION_SCHEDULE_MANAGE')")
    public ResponseEntity<Page<Participant>> listParticipants(@SortDefault.SortDefaults({
                @SortDefault(sort = "lastname", direction = org.springframework.data.domain.Sort.Direction.ASC),
                @SortDefault(sort = "firstname", direction = org.springframework.data.domain.Sort.Direction.ASC)
            }) @PageableDefault(size = 20) final Pageable pageable, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(participantService.listParticipants(pageable, search));
    }

    @GetMapping("/fetch/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_SCHEDULE_MANAGE')")
    public ResponseEntity<Participant> getParticipant(@PathVariable String id) {
        return ResponseEntity.ok(participantService.getParticipant(id));
    }

    @GetMapping("/fetchByMail/{email:.+}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'PERMISSION_SCHEDULE_MANAGE')")
    public ResponseEntity<Participant> getParticipantByEmail(@PathVariable String email) {
        return ResponseEntity.ok(participantService.getParticipantByEmail(email));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('PERMISSION_SCHEDULE_MANAGE')")
    public ResponseEntity<Participant> createOrUpdateParticipant(@RequestBody Participant participant) {
        return ResponseEntity.ok(participantService.createOrUpdateParticipant(participant));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_SCHEDULE_MANAGE')")
    public ResponseEntity<Void> deleteParticipant(@PathVariable String id) {
        participantService.deleteParticipant(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERMISSION_SCHEDULE_MANAGE')")
    public ResponseEntity<ParticipantDiff> importParticipants(@RequestBody MultipartFile file) {
        try {
            return ResponseEntity.ok(participantService.importParticipants(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
