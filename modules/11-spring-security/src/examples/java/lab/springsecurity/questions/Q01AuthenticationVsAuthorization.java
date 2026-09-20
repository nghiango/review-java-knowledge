package lab.springsecurity.questions;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class Q01AuthenticationVsAuthorization {

    public static void main(String[] args) {
        // Authentication: Verifying WHO you are (Identity)
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        "alice",
                        "credentials_hidden",
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("SCOPE_read")));

        boolean isAuthenticated = auth.isAuthenticated(); // true
        String principalName = auth.getName(); // "alice"

        // Authorization: Verifying WHAT you are permitted to do (Access Rights)
        boolean hasAdminRole =
                auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())); // false
        boolean hasReadScope =
                auth.getAuthorities().stream()
                        .anyMatch(a -> "SCOPE_read".equals(a.getAuthority())); // true

        System.out.println("Authenticated: " + isAuthenticated); // Authenticated: true
        System.out.println("Principal: " + principalName); // Principal: alice
        System.out.println("Has Admin Role: " + hasAdminRole); // Has Admin Role: false
        System.out.println("Has Read Scope: " + hasReadScope); // Has Read Scope: true
    }
}
