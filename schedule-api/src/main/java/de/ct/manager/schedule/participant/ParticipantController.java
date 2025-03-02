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
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/participant")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Participant>> listParticipants() {
        return ResponseEntity.ok(participantService.listParticipants());
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Participant>> listCompanies(@SortDefault(sort = "lastname") @PageableDefault(size = 20) final Pageable pageable) {
        return ResponseEntity.ok(participantService.listParticipants(pageable));
    }

    @GetMapping("/fetch/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Participant> getParticipant(@PathVariable String id) {
        return ResponseEntity.ok(participantService.getParticipant(id));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Participant> createOrUpdateParticipant(@RequestBody Participant participant) {
        return ResponseEntity.ok(participantService.createOrUpdateParticipant(participant));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteParticipant(@PathVariable String id) {
        participantService.deleteParticipant(id);
        return ResponseEntity.ok().build();
    }
}
