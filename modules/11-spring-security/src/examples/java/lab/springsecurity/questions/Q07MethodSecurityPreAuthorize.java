package lab.springsecurity.questions;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class Q07MethodSecurityPreAuthorize {

    public static void main(String[] args) {
        // Simulating SpEL evaluation: "@PreAuthorize(\"hasRole('ADMIN') or #owner ==
        // authentication.name\")"
        Authentication aliceAuth =
                new UsernamePasswordAuthenticationToken(
                        "alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication bobAuth =
                new UsernamePasswordAuthenticationToken(
                        "bob", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication adminAuth =
                new UsernamePasswordAuthenticationToken(
                        "superadmin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String documentOwner = "alice";

        boolean aliceAllowed = evaluateSpEL(aliceAuth, documentOwner); // true (owner match)
        boolean bobAllowed =
                evaluateSpEL(bobAuth, documentOwner); // false (owner mismatch & no admin role)
        boolean adminAllowed = evaluateSpEL(adminAuth, documentOwner); // true (hasRole ADMIN)

        System.out.println("Alice (owner) access: " + aliceAllowed); // Alice (owner) access: true
        System.out.println(
                "Bob (non-owner) access: " + bobAllowed); // Bob (non-owner) access: false
        System.out.println("Admin access: " + adminAllowed); // Admin access: true
    }

    public static boolean evaluateSpEL(Authentication auth, String owner) {
        boolean isAdmin =
                auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return isAdmin || owner.equals(auth.getName());
    }
}
