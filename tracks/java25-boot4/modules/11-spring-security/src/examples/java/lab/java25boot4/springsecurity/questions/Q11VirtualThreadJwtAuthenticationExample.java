package lab.java25boot4.springsecurity.questions;

import java.util.Set;

/**
 * Q11: How do you perform high-concurrency stateless JWT authentication on virtual threads without
 * thread pinning?
 */
public class Q11VirtualThreadJwtAuthenticationExample {

    public record JwtAuthenticationToken(String subject, Set<String> roles, boolean isValid) {}

    public static JwtAuthenticationToken parseAndValidateToken(String token) {
        if (token != null && token.startsWith("eyJh")) {
            return new JwtAuthenticationToken("usr_prod_100", Set.of("ROLE_USER"), true);
        }
        return new JwtAuthenticationToken("anonymous", Set.of(), false);
    }

    public static void main(String[] args) {
        String mockJwt = "eyJhGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        var auth = parseAndValidateToken(mockJwt);

        System.out.println("Subject: " + auth.subject()); // "usr_prod_100"
        System.out.println("Valid: " + auth.isValid()); // true
        System.out.println("Roles: " + auth.roles().contains("ROLE_USER")); // true
    }
}
