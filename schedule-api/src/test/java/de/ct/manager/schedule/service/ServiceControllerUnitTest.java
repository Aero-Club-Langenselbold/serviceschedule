package de.ct.manager.schedule.service;

import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.ct.manager.schedule.user.UserRole;
import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(ServiceController.class)
@Import({
    ServiceService.class
})
class ServiceControllerUnitTest extends AbstractControllerUnitTest{

    @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private ServiceDateRepository serviceDateRepository;

    @Test
    void testListParticipants() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        when(serviceRepository.findAll()).thenReturn(generateListResponse());

        mockMvc.perform(
            get("/service/list")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].servicename", is("Service No. 0")));
    }

    @Test
    void testService() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        when(serviceRepository.findById("4711")).thenReturn(Optional.of(generateService(0)));

        mockMvc.perform(
            get("/service/fetch/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.servicename", is("Service No. 0")));
    }

    @Test
    void testCreateService() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        Service service = generateService(0);

        when(serviceRepository.save(service)).thenReturn(service);

        mockMvc.perform(
            post("/service/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(service))
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.servicename", is("Service No. 0")));
    }

    @Test
    void testDeleteService() throws Exception {
        mockValidAuthentication(UserRole.ROLE_ADMIN);

        mockMvc.perform(
            delete("/service/delete/4711")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateValidJwtToken(UserRole.ROLE_ADMIN))
            )
            .andDo(print())
            .andExpect(status().isOk());
    }

    private List<Service> generateListResponse() {
        List<Service> services = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            services.add(generateService(i));
        }

        return services;
    }

    private Service generateService(int index) {
        Service service = new Service();
        service.setServicename("Service No. " + index);

        return service;
    }
}
