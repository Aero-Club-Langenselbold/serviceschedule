package de.ct.manager.schedule.featureflag;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class FeatureFlagRepositoryUnitTest {

    @Autowired
    FeatureFlagRepository featureFlagRepository;

    @BeforeEach
    void setup() {
        FeatureFlag flag = new FeatureFlag();
        flag.setId("reallyuniqueid");
        flag.setActive(1);
        flag.setDescription("good flag");
        featureFlagRepository.save(flag);

        flag = new FeatureFlag();
        flag.setId("reallyuniqueid#2");
        flag.setActive(0);
        flag.setDescription("bad flag");
        featureFlagRepository.save(flag);
    }

    @Test
    void findFlag() {
        Assertions.assertThat(featureFlagRepository.findAllByActive(1)).hasSize(1);
        Assertions.assertThat(featureFlagRepository.findAllByActive(0)).hasSize(1);
    }

    @AfterEach
    void destroy() {
        featureFlagRepository.deleteAll();
    }
}
