package lab.springsecurity.questions;

import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Q26: How do you implement fine-grained, dynamic ABAC (Attribute-Based Access Control) using
 * Spring Security 6's AuthorizationManager?
 */
public class Q26DynamicAttributeAuthorizationManager {

    // Context carrying request attributes: tenant ID, client IP, resource sensitivity
    record ResourceContext(String tenantId, String clientIp, int sensitivityLevel) {}

    public static void main(String[] args) {
        // ABAC Authorization Manager checking caller tenant and resource sensitivity
        AuthorizationManager<ResourceContext> abacManager =
                (authenticationSupplier, context) -> {
                    Authentication auth = authenticationSupplier.get();
                    if (auth == null || !auth.isAuthenticated()) {
                        return new AuthorizationDecision(false);
                    }

                    // Attribute rule: Caller must match tenant and have sufficient clearance level
                    boolean tenantMatch = auth.getName().startsWith(context.tenantId() + ":");
                    boolean hasClearance =
                            auth.getAuthorities().stream()
                                    .anyMatch(
                                            a ->
                                                    "CLEARANCE_HIGH".equals(a.getAuthority())
                                                            || context.sensitivityLevel() < 3);

                    return new AuthorizationDecision(tenantMatch && hasClearance);
                };

        Authentication matchingUser =
                new UsernamePasswordAuthenticationToken(
                        "acme:charlie",
                        "cred",
                        List.of(new SimpleGrantedAuthority("CLEARANCE_HIGH")));
        Authentication wrongTenantUser =
                new UsernamePasswordAuthenticationToken(
                        "othercorp:bob",
                        "cred",
                        List.of(new SimpleGrantedAuthority("CLEARANCE_HIGH")));

        ResourceContext confidentialDoc = new ResourceContext("acme", "10.0.0.1", 3);

        boolean allowedMatching =
                abacManager.check(() -> matchingUser, confidentialDoc).isGranted(); // true
        boolean allowedWrongTenant =
                abacManager.check(() -> wrongTenantUser, confidentialDoc).isGranted(); // false

        System.out.println("Matching Tenant & Clearance Granted: " + allowedMatching); // true
        System.out.println("Cross-Tenant Access Denied: " + !allowedWrongTenant); // true
    }
}
