package de.ct.manager.schedule.user;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

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
        User user = new User();
        user.setUsername("test");
        user.setPassword("password");
        user.setId("1234");
        user.setUserRole(UserRole.ROLE_USER);

        when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));

        UserDetails ud = userDetailsService.loadUserByUsername("test");

        Assertions.assertThat(ud).isNotNull();
        Assertions.assertThat(ud.getUsername()).isEqualTo("test");
        Assertions.assertThat(ud.getPassword()).isEqualTo("password");
    }

    @Test
    void testUsernameNotFound() {
        when(userRepository.findByUsername("test")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
            userDetailsService.loadUserByUsername("test"));

    }
}