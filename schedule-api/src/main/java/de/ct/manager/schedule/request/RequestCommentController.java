package de.ct.manager.schedule.request;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.user.UserDetailsImpl;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/request/{requestId}/comments")
public class RequestCommentController {

    private final RequestCommentService commentService;

    public RequestCommentController(RequestCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<RequestCommentResponse>> getComments(@PathVariable String requestId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(commentService.getComments(requestId, currentUser));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SCHEDULE_ADMIN', 'ADMIN')")
    public ResponseEntity<RequestCommentResponse> addComment(@PathVariable String requestId, @Valid @RequestBody RequestCommentRequest comment, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(commentService.addComment(requestId, comment.getContent(), currentUser));
    }
}
