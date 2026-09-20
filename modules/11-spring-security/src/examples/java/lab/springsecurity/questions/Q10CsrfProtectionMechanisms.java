package lab.springsecurity.questions;

public class Q10CsrfProtectionMechanisms {

    public static void main(String[] args) {
        // Synchronizer Token Pattern (CSRF Token)
        String expectedSessionCsrfToken = "csrf-token-secret-uuid-9988";

        // Mutating Request 1: Valid X-XSRF-TOKEN header provided by SPA
        String headerTokenValid = "csrf-token-secret-uuid-9988";
        boolean request1Allowed = expectedSessionCsrfToken.equals(headerTokenValid); // true

        // Mutating Request 2: Cross-site request triggered by malicious <img> or <form> (no custom
        // header)
        String headerTokenMissing = null;
        boolean request2Blocked =
                (headerTokenMissing == null
                        || !expectedSessionCsrfToken.equals(headerTokenMissing)); // true

        // SameSite cookie policy
        String cookieAttribute = "SameSite=Strict; HttpOnly; Secure";
        boolean hasSameSiteStrict = cookieAttribute.contains("SameSite=Strict"); // true

        System.out.println(
                "Legitimate SPA request with CSRF token allowed: "
                        + request1Allowed); // Legitimate SPA request with CSRF token allowed: true
        System.out.println(
                "Forged cross-site request blocked: "
                        + request2Blocked); // Forged cross-site request blocked: true
        System.out.println(
                "SameSite=Strict defense configured: "
                        + hasSameSiteStrict); // SameSite=Strict defense configured: true
    }
}
