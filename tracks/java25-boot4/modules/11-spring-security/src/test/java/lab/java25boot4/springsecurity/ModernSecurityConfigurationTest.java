package lab.java25boot4.springsecurity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

class ModernSecurityConfigurationTest {

    private final ModernSecurityConfiguration config = new ModernSecurityConfiguration();

    @Test
    @DisplayName("passwordEncoder encodes and verifies passwords via BCrypt")
    void passwordEncoderShouldVerifyCredentials() {
        PasswordEncoder encoder = config.passwordEncoder();

        String encoded = encoder.encode("secret123");

        assertThat(encoded).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", encoded)).isTrue();
        assertThat(encoder.matches("wrong_secret", encoded)).isFalse();
    }

    @Test
    @DisplayName("userDetailsService provisions regular user and admin with correct authorities")
    void userDetailsServiceShouldProvisionConfiguredUsers() {
        PasswordEncoder encoder = config.passwordEncoder();
        UserDetailsService userDetailsService = config.userDetailsService(encoder);

        UserDetails user = userDetailsService.loadUserByUsername("user");
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(user.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        assertThat(admin.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
