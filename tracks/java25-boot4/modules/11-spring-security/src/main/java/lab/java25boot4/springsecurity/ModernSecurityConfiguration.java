package lab.java25boot4.springsecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Modern Spring Security 7 configuration demonstrating pure lambda-based DSL, secure-by-default
 * matcher ordering, and stateless session management.
 */
@Configuration
@EnableWebSecurity
public class ModernSecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/public/**")
                                        .permitAll()
                                        .requestMatchers("/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/**")
                                        .hasAnyRole("USER", "ADMIN")
                                        .anyRequest()
                                        .denyAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails regularUser =
                User.builder()
                        .username("user")
                        .password(encoder.encode("password123"))
                        .roles("USER")
                        .build();

        UserDetails adminUser =
                User.builder()
                        .username("admin")
                        .password(encoder.encode("admin123"))
                        .roles("ADMIN")
                        .build();

        return new InMemoryUserDetailsManager(regularUser, adminUser);
    }
}
