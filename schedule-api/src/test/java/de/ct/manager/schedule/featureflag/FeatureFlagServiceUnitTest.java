package de.ct.manager.schedule.featureflag;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class FeatureFlagServiceUnitTest {

    @Mock
    FeatureFlagRepository featureFlagRepository;

    @InjectMocks
    FeatureFlagService featureFlagService;

    @Test
    void testLoadFeatureFlags() {
        when(featureFlagRepository.findAllByActive(1)).thenReturn(generateListResponse());

        List<FeatureFlag> result = featureFlagService.getActiveFeatureFlags();

        Assertions.assertThat(result).isNotNull().hasSize(10);
    }

    private List<FeatureFlag> generateListResponse() {
        List<FeatureFlag> flags = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            FeatureFlag flag = new FeatureFlag();
            flag.setId("fancyid" + i);
            flag.setActive(1);
            flags.add(flag);
        }

        return flags;
    }
}
