package de.ct.manager.schedule.request;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Request createOrUpdateRequest(Request request) {
        if (request.getId() == null || request.getId().isEmpty()) {
            request.setId(UUID.randomUUID().toString());
        } else {
            Request existingRequest = requestRepository.findById(request.getId()).orElse(null);
            if (existingRequest != null) {
                request.setAuthor(existingRequest.getAuthor());
                request.setCreatedAt(existingRequest.getCreatedAt());
            }
        }
        return requestRepository.save(request);
    }

    public Request getRequestById(String id) {
        return requestRepository.findById(id).orElse(null);
    }

    public void deleteRequest(String id) {
        requestRepository.deleteById(id);
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAll(Sort.by("createdAt").descending());
    }

    public Page<Request> getRequestsByPage(Pageable pageable) {
        return requestRepository.findAll(pageable);
    }
}
