package lab.springsecurity.questions;

import java.util.Map;

/**
 * Q30: Production Incident: OAuth2 Token relay causes infinite redirect loop and token leakage
 * behind TLS-terminating reverse proxy. How do you triage and resolve?
 */
public class Q30OAuth2RedirectLoopTlsTerminationIncident {

    public static void main(String[] args) {
        // Reverse proxy headers sent downstream to Spring Boot application
        Map<String, String> ingressHeaders =
                Map.of(
                        "X-Forwarded-Proto", "https",
                        "X-Forwarded-Host", "app.fintech.com",
                        "X-Forwarded-Port", "443");

        // Without server.forward-headers-strategy=FRAMEWORK:
        // App thinks request is "http://app.fintech.com/oauth2/authorization/login"
        String rawScheme = "http";
        boolean triggersHttpToHttpsLoop = "http".equals(rawScheme); // true

        // With ForwardedHeaderFilter / server.forward-headers-strategy=FRAMEWORK:
        String resolvedScheme = ingressHeaders.getOrDefault("X-Forwarded-Proto", "http");
        String resolvedHost = ingressHeaders.getOrDefault("X-Forwarded-Host", "localhost");
        String redirectUri = resolvedScheme + "://" + resolvedHost + "/login/oauth2/code/oidc";

        boolean isSecureHttps = redirectUri.startsWith("https://"); // true

        System.out.println("Raw Scheme Loop Trigger: " + triggersHttpToHttpsLoop); // true
        System.out.println("Resolved Secure Redirect URI: " + redirectUri); // https://app.fintech.com/login/oauth2/code/oidc
        System.out.println("Resolved Scheme is HTTPS: " + isSecureHttps); // true
    }
}
