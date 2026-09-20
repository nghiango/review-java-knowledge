package lab.springsecurity.passwords;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SafePasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Uses BCrypt by default with support for Argon2 and SCrypt migration
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
