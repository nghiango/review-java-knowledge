package lab.springsecurity.questions;

import java.util.List;

public class Q11CorsConfigurationRules {

    public static void main(String[] args) {
        List<String> allowedOrigins =
                List.of("https://app.example.com", "https://admin.example.com");
        boolean allowCredentials = true;

        // Insecure configuration check: Wildcard pattern with allowCredentials=true
        String wildcardPattern = "*";
        boolean isInsecureCombination =
                "*".equals(wildcardPattern) && allowCredentials; // true (dangerous!)

        // Secure evaluation: Explicit whitelist check
        String incomingOrigin = "https://app.example.com";
        boolean isOriginAllowed = allowedOrigins.contains(incomingOrigin); // true

        String untrustedOrigin = "https://evil-attacker.com";
        boolean isUntrustedRejected = !allowedOrigins.contains(untrustedOrigin); // true

        System.out.println(
                "Wildcard + Credentials is insecure: "
                        + isInsecureCombination); // Wildcard + Credentials is insecure: true
        System.out.println(
                "Trusted origin permitted: " + isOriginAllowed); // Trusted origin permitted: true
        System.out.println(
                "Untrusted origin rejected: "
                        + isUntrustedRejected); // Untrusted origin rejected: true
    }
}
