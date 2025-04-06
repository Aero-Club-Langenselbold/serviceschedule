package de.ct.manager.schedule.request;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Request createOrUpdateRequest(Request request) {
        return requestRepository.save(request);
    }

    public Request getRequestById(String id) {
        return requestRepository.findById(id).orElse(null);
    }

    public void deleteRequest(String id) {
        requestRepository.deleteById(id);
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }

    public Page<Request> getRequestsByPage(Pageable pageable) {
        return requestRepository.findAll(pageable);
    }
}
