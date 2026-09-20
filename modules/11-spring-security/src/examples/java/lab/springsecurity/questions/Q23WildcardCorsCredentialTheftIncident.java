package lab.springsecurity.questions;

import java.util.List;

public class Q23WildcardCorsCredentialTheftIncident {

    public static void main(String[] args) {
        // Incident: Developer configured allowedOriginPatterns("*") with allowCredentials(true)
        boolean allowCredentials = true;
        List<String> allowedOriginPatterns = List.of("*");

        String attackerOrigin = "https://malicious-phishing-site.com";

        // Vulnerable browser preflight check
        boolean vulnerableAcceptsAnyOrigin =
                allowedOriginPatterns.contains("*")
                        && allowCredentials; // true (Session Hijacking / CSRF vulnerability)

        // Remediation: Strict Origin Whitelisting
        List<String> strictWhitelist = List.of("https://dashboard.company.com");
        boolean attackerOriginBlocked = !strictWhitelist.contains(attackerOrigin); // true

        System.out.println(
                "Vulnerable configuration allowed attacker origin: "
                        + vulnerableAcceptsAnyOrigin); // Vulnerable configuration allowed attacker
        // origin: true
        System.out.println(
                "Strict whitelist blocked malicious origin: "
                        + attackerOriginBlocked); // Strict whitelist blocked malicious origin: true
    }
}
