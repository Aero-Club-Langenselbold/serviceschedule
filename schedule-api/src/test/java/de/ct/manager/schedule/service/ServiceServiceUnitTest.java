package de.ct.manager.schedule.service;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ServiceServiceUnitTest {

    @Mock
    ServiceRepository serviceRepository;

    @InjectMocks
    ServiceService serviceService;

    @Test
    void testLoadServicesList() {
        when(serviceRepository.findAll()).thenReturn(generateListResponse());

        List<Service> result = serviceService.listServices();

        Assertions.assertThat(result).isNotNull().hasSize(10);
    }

    @Test
    void testGetService() {
        Service service = new Service();
        service.setServicename("Fancy Service");

        when(serviceRepository.findById("1")).thenReturn(Optional.of(service));
        when(serviceRepository.findById("0")).thenReturn(Optional.empty());

        Assertions.assertThat(serviceService.getService("1")).isNotNull();
        Assertions.assertThat(serviceService.getService("0")).isNull();
    }

    @Test
    void testCreateOrUpdateService() {
        Service service = new Service();
        service.setServicename("Fancy Service");

        when(serviceRepository.save(service)).thenReturn(service);

        Assertions.assertThat(serviceService.createOrUpdateService(service)).isNotNull();
    }

    @Test
    void testDeleteService() {
        Service service = new Service();
        service.setServicename("Fancy Service");

        when(serviceRepository.findById("1")).thenReturn(Optional.of(service));
        when(serviceRepository.findById("0")).thenReturn(Optional.empty());

        serviceService.deleteService("1");
        serviceService.deleteService("0");

        Assertions.assertThatNoException();
    }

    private List<Service> generateListResponse() {
        List<Service> services = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Service service = new Service();

            services.add(service);
        }

        return services;
    }
}
