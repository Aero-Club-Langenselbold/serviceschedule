package de.ct.manager.schedule.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceDateRepository serviceDateRepository;

    public ServiceService(ServiceRepository serviceRepository, ServiceDateRepository serviceDateRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceDateRepository = serviceDateRepository;
    }

    public List<Service> listServices() {
        return serviceRepository.findAll(Sort.by("servicename").ascending());
    }

    public Service getService(String id) {
        return serviceRepository.findById(id).orElse(null);
    }

    public Service createOrUpdateService(Service service) {
        if (service.getId().isEmpty()) {
            service.setId(UUID.randomUUID().toString());
        }
        if (service.getSettings() != null && !service.getSettings().isEmpty()) {
            service.getSettings().forEach(settings -> settings.setService(service));
        }

        Optional<List<ServiceDate>> existingServiceDates = serviceDateRepository.findByServiceId(service.getId());
        service.setServiceDates(existingServiceDates.orElse(null));

        return serviceRepository.save(service);
    }

    public void deleteService(String id) {
        serviceRepository.deleteById(id);
    }
}

