package de.ct.manager.schedule.user;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class UserDetailsServiceUnitTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserDetailsServiceImpl userDetailsService;

    @Test
    void testLoadByUsername() {
        Role role = new Role();
        role.setName(UserRole.ROLE_USER);

        User user = new User();
        user.setUsername("test");
        user.setPassword("password");
        user.setId("1234");
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));

        UserDetails ud = userDetailsService.loadUserByUsername("test");

        Assertions.assertThat(ud).isNotNull();
        Assertions.assertThat(ud.getUsername()).isEqualTo("test");
        Assertions.assertThat(ud.getPassword()).isEqualTo("password");
        Assertions.assertThat(ud.getAuthorities()).hasSize(1);
        Assertions.assertThat(ud.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void testLoadByUsernameMultipleRoles() {
        Role roleUser = new Role();
        roleUser.setName(UserRole.ROLE_USER);

        Role roleAdmin = new Role();
        roleAdmin.setName(UserRole.ROLE_ADMIN);

        User user = new User();
        user.setUsername("admin");
        user.setPassword("password");
        user.setId("5678");
        user.setRoles(Set.of(roleUser, roleAdmin));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails ud = userDetailsService.loadUserByUsername("admin");

        Assertions.assertThat(ud).isNotNull();
        Assertions.assertThat(ud.getAuthorities()).hasSize(2);
    }

    @Test
    void testUsernameNotFound() {
        when(userRepository.findByUsername("test")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
            userDetailsService.loadUserByUsername("test"));

    }
}