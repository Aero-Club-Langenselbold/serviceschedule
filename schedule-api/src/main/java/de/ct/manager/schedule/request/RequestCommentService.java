package de.ct.manager.schedule.request;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRepository;
import de.ct.manager.schedule.user.UserRole;

@org.springframework.stereotype.Service
public class RequestCommentService {

    private final RequestRepository requestRepository;
    private final RequestCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public RequestCommentService(RequestRepository requestRepository, RequestCommentRepository commentRepository, UserRepository userRepository, ParticipantRepository participantRepository) {
        this.requestRepository = requestRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional(readOnly = true)
    public List<RequestCommentResponse> getComments(String requestId, UserDetailsImpl currentUser) {
        Request request = findAccessibleRequest(requestId, currentUser);
        List<RequestComment> comments = commentRepository.findAllByRequestOrderByCreatedAtAsc(request);

        List<String> usernames = comments.stream()
                .filter(comment -> comment.getAuthor() != null)
                .map(comment -> comment.getAuthor().getUsername())
                .distinct()
                .toList();
        Map<String, String> fullnames = participantRepository.findAllByEmailIn(usernames).stream()
                .collect(Collectors.toMap(Participant::getEmail, Participant::getFullname, (first, second) -> first));

        return comments.stream().map(comment -> toResponse(comment, fullnames)).toList();
    }

    @Transactional
    public RequestCommentResponse addComment(String requestId, String content, UserDetailsImpl currentUser) {
        Request request = findAccessibleRequest(requestId, currentUser);
        if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only open requests can be commented");
        }
        User author = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));

        RequestComment comment = new RequestComment();
        comment.setRequest(request);
        comment.setAuthor(author);
        comment.setContent(content.strip());
        RequestComment saved = commentRepository.save(comment);

        String fullname = participantRepository.findByEmail(author.getUsername()).map(Participant::getFullname).orElse(null);
        return toResponse(saved, fullname == null ? Map.of() : Map.of(author.getUsername(), fullname));
    }

    // Same visibility as the request list: administrators see everything, everybody else only their own requests.
    private Request findAccessibleRequest(String requestId, UserDetailsImpl currentUser) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(authority -> UserRole.ROLE_ADMIN.name().equals(authority.getAuthority()));
        boolean isAuthor = request.getAuthor() != null && currentUser.getUsername().equals(request.getAuthor().getEmail());
        if (!isAdmin && !isAuthor) {
            throw new AccessDeniedException("Request belongs to another user");
        }
        return request;
    }

    private RequestCommentResponse toResponse(RequestComment comment, Map<String, String> fullnames) {
        String username = comment.getAuthor() != null ? comment.getAuthor().getUsername() : null;
        return new RequestCommentResponse(comment.getId(), comment.getContent(), comment.getCreatedAt(), fullnames.getOrDefault(username, username));
    }
}
