package de.ct.manager.schedule.user;

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
class RoleRepositoryUnitTest {

    @Autowired
    RoleRepository roleRepository;

    @BeforeEach
    void setup() {
        Role user = new Role();
        user.setName(UserRole.ROLE_USER);
        roleRepository.save(user);

        Role admin = new Role();
        admin.setName(UserRole.ROLE_ADMIN);
        roleRepository.save(admin);
    }

    @AfterEach
    void destroy() {
        roleRepository.deleteAll();
    }

    @Test
    void findRoleUser() {
        Assertions.assertThat(roleRepository.findByName(UserRole.ROLE_USER)).isPresent();
    }

    @Test
    void findRoleAdmin() {
        Assertions.assertThat(roleRepository.findByName(UserRole.ROLE_ADMIN)).isPresent();
    }

    @Test
    void findOnlyTwoRoles() {
        Assertions.assertThat(roleRepository.findAll()).hasSize(2);
    }
}
