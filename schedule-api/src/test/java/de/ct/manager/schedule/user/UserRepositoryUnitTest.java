package de.ct.manager.schedule.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import org.assertj.core.api.Assertions;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class UserRepositoryUnitTest {

    private static final String TEST_USER = "admin@world.com";
    private static final String TEST_PW = "pwned";

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.save(new User(TEST_USER, TEST_PW));
    }

    @AfterEach
    void destroy() {
        userRepository.deleteAll();
    }

    @Test
    void findUser() {
        Assertions.assertThat(userRepository.findByUsername(TEST_USER)).isPresent();
        Assertions.assertThat(userRepository.existsByUsername(TEST_USER)).isTrue();
    }

    @Test
    void persistsSeveralPermissions() {
        User user = userRepository.findByUsername(TEST_USER).orElseThrow();
        user.getPermissions().add(UserPermission.PERMISSION_SCHEDULE_MANAGE);
        user.getPermissions().add(UserPermission.PERMISSION_ALARM_MANAGE);
        userRepository.saveAndFlush(user);

        Assertions.assertThat(userRepository.findByUsername(TEST_USER).orElseThrow().getPermissions())
                .containsExactlyInAnyOrder(UserPermission.PERMISSION_SCHEDULE_MANAGE, UserPermission.PERMISSION_ALARM_MANAGE);
    }

    @Test
    void searchMatchesUsernameCaseInsensitive() {
        Assertions.assertThat(userRepository.findAll(PageRequest.of(0, 10), "ADMIN@WORLD").getContent()).hasSize(1);
        Assertions.assertThat(userRepository.findAll(PageRequest.of(0, 10), "nobody").getContent()).isEmpty();
    }

    @Test
    void findNonExistingUser() {
        Assertions.assertThat(userRepository.findByUsername("MrIncognito")).isEmpty();
        Assertions.assertThat(userRepository.existsByUsername("MrIncognito")).isFalse();
    }
}
