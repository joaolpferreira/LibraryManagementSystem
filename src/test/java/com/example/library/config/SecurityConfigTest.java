package com.example.library.config;

import java.time.Clock;
import java.util.Optional;

import com.example.library.user.LibraryUser;
import com.example.library.user.LibraryUserRepository;
import com.example.library.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void passwordEncoderProducesVerifiableDelegatingHashes() {
        SecurityConfig config = new SecurityConfig();
        String encoded = config.passwordEncoder().encode("secret");

        assertThat(encoded).startsWith("{bcrypt}");
        assertThat(config.passwordEncoder().matches("secret", encoded)).isTrue();
    }

    @Test
    void userDetailsServiceMapsEnabledAndDisabledUsersAndUnknownUsername() {
        LibraryUserRepository repository = mock(LibraryUserRepository.class);
        SecurityConfig config = new SecurityConfig();
        UserDetailsService service = config.userDetailsService(repository);

        LibraryUser enabled = user("owner", UserRole.OWNER, true);
        LibraryUser disabled = user("client", UserRole.CLIENT, false);
        when(repository.findByUsername("owner")).thenReturn(Optional.of(enabled));
        when(repository.findByUsername("client")).thenReturn(Optional.of(disabled));
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());

        UserDetails owner = service.loadUserByUsername("owner");
        UserDetails client = service.loadUserByUsername("client");

        assertThat(owner.getUsername()).isEqualTo("owner");
        assertThat(owner.getPassword()).isEqualTo("{noop}password");
        assertThat(owner.isEnabled()).isTrue();
        assertThat(owner.getAuthorities()).extracting("authority").containsExactly("ROLE_OWNER");
        assertThat(client.isEnabled()).isFalse();
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Unknown user missing");
    }

    @Test
    void applicationClockUsesUtc() {
        Clock clock = new ApplicationConfig().clock();

        assertThat(clock.getZone()).isEqualTo(java.time.ZoneOffset.UTC);
    }

    private static LibraryUser user(String username, UserRole role, boolean enabled) {
        LibraryUser user = mock(LibraryUser.class);
        when(user.getUsername()).thenReturn(username);
        when(user.getPasswordHash()).thenReturn("{noop}password");
        when(user.getRole()).thenReturn(role);
        when(user.isEnabled()).thenReturn(enabled);
        return user;
    }
}
