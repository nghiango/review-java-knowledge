package lab.springsecurity.questions;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Q28: How does Spring Security authenticate client certificates via X509AuthenticationFilter
 * and validate Subject Alternative Names (SANs)?
 */
public class Q28MtlsX509CertificateAuthentication {

    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

    public static void main(String[] args) {
        // Simulated client X.509 Subject DN presented during mTLS handshake
        String subjectDn = "CN=order-service.internal, OU=Payments, O=FintechCorp, C=US";

        Matcher matcher = CN_PATTERN.matcher(subjectDn);
        String commonName = matcher.find() ? matcher.group(1) : ""; // "order-service.internal"

        // Map certificate identity to authenticated principal and service authorities
        PreAuthenticatedAuthenticationToken mtlsAuth =
                new PreAuthenticatedAuthenticationToken(
                        commonName,
                        "N/A (mTLS Verified at TLS Layer)",
                        List.of(
                                new SimpleGrantedAuthority("ROLE_TRUSTED_SERVICE"),
                                new SimpleGrantedAuthority("SCOPE_orders:write")));

        boolean isMtlsAuthenticated = mtlsAuth.isAuthenticated(); // true
        boolean isInternalService =
                "order-service.internal".equals(mtlsAuth.getPrincipal()); // true
        boolean hasWriteScope =
                mtlsAuth.getAuthorities().stream()
                        .anyMatch(a -> "SCOPE_orders:write".equals(a.getAuthority())); // true

        System.out.println("mTLS Authenticated: " + isMtlsAuthenticated); // true
        System.out.println("Common Name: " + commonName); // order-service.internal
        System.out.println("Internal Service Match: " + isInternalService); // true
        System.out.println("Write Scope Present: " + hasWriteScope); // true
    }
}
