package lab.springsecurity.csrf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SafeStatelessSecurityConfig {

    @Bean
    public SecurityFilterChain statelessSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF safely disabled ONLY because session creation is strictly STATELESS and no
                // session cookies are stored
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }
}
