package lab.springsecurity.questions;

import java.util.Map;
import java.util.function.Function;

/**
 * Q27: How do you configure a Spring Security OAuth2 Resource Server to dynamically resolve
 * multiple tenant JWKS issuers at runtime?
 */
public class Q27MultiTenantJwtIssuerRouting {

    // Resolves JWKS public key endpoint from the JWT's "iss" claim
    public static void main(String[] args) {
        Map<String, String> tenantJwksRegistry =
                Map.of(
                        "https://auth.tenant-a.com/oauth2", "https://auth.tenant-a.com/.well-known/jwks.json",
                        "https://auth.tenant-b.com/oauth2", "https://auth.tenant-b.com/.well-known/jwks.json");

        Function<String, String> dynamicIssuerResolver =
                issuerClaim -> {
                    String jwksUri = tenantJwksRegistry.get(issuerClaim);
                    if (jwksUri == null) {
                        throw new IllegalArgumentException("Untrusted or unknown token issuer: " + issuerClaim);
                    }
                    return jwksUri;
                };

        String validIssuer = "https://auth.tenant-a.com/oauth2";
        String resolvedJwks = dynamicIssuerResolver.apply(validIssuer);
        boolean isCorrectJwks =
                "https://auth.tenant-a.com/.well-known/jwks.json".equals(resolvedJwks); // true

        String untrustedIssuer = "https://evil-idp.attacker.com";
        boolean rejectedUntrusted = false;
        try {
            String unused = dynamicIssuerResolver.apply(untrustedIssuer);
        } catch (IllegalArgumentException e) {
            rejectedUntrusted = true; // true (unregistered issuers fail-closed)
        }

        System.out.println("Resolved JWKS URI: " + resolvedJwks);
        System.out.println("Valid JWKS Match: " + isCorrectJwks); // true
        System.out.println("Untrusted Issuer Rejected: " + rejectedUntrusted); // true
    }
}
