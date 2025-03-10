package de.ct.manager.schedule.service;

import java.util.List;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<Service> listServices() {
        return serviceRepository.findAll();
    }

    public Service getService(String id) {
        return serviceRepository.findById(id).orElse(null);
    }

    public Service createOrUpdateService(Service service) {
        if (!service.getSettings().isEmpty()) {
            service.getSettings().forEach(settings -> settings.setService(service));
        }
        return serviceRepository.save(service);
    }

    public void deleteService(String id) {
        serviceRepository.deleteById(id);
    }
}
