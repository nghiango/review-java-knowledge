package lab.java25boot4.springsecurity.questions;

import java.util.List;

/**
 * Q09: How do you refactor a legacy Spring Security configuration using and() chaining into the
 * modern Spring Security 7 lambda DSL?
 */
public class Q09MigrationSecurity6To7DslRefactorExample {

    public record MigrationComparison(String legacySyntax, String modernSyntax) {}

    public static void main(String[] args) {
        var comparison =
                List.of(
                        new MigrationComparison(
                                "http.csrf().disable().and().authorizeRequests()",
                                "http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth...)"),
                        new MigrationComparison(
                                "http.httpBasic().and().sessionManagement().sessionCreationPolicy(...)",
                                "http.httpBasic(Customizer.withDefaults()).sessionManagement(session -> session.sessionCreationPolicy(...))"));

        System.out.println("Migration rules count: " + comparison.size()); // 2
        System.out.println("Modern DSL uses lambdas: " + true); // true
    }
}
