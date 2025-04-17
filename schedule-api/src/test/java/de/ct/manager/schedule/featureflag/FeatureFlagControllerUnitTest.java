package de.ct.manager.schedule.featureflag;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.ct.manager.schedule.util.AbstractControllerUnitTest;

@WebMvcTest(FeatureFlagController.class)
@Import({
    FeatureFlagService.class
})
class FeatureFlagControllerUnitTest extends AbstractControllerUnitTest {

    @MockitoBean
    private FeatureFlagRepository featureFlagRepository;

    @Test
    void testListFlags() throws Exception {
        when(featureFlagRepository.findAllByActive(1)).thenReturn(generateListResponse());

        mockMvc.perform(
            get("/featureflag/active")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id", is("fancyid 0")));
    }


    private List<FeatureFlag> generateListResponse() {
        List<FeatureFlag> flags = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            FeatureFlag flag = new FeatureFlag();
            flag.setId("fancyid " + i);
            flag.setActive(1);
            flags.add(flag);
        }

        return flags;
    }
}
