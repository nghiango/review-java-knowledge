package lab.springsecurity.questions;

import java.util.Map;

/**
 * Q24: How does Spring Security configure HSTS, Content-Security-Policy (CSP), and X-Frame-Options
 * to mitigate clickjacking and XSS?
 */
public class Q24SecurityHeadersHstsCspXFrame {

    public static void main(String[] args) {
        // Default and hardened security headers emitted by HeaderWriterFilter
        Map<String, String> standardHeaders =
                Map.of(
                        "Strict-Transport-Security", "max-age=31536000 ; includeSubDomains",
                        "X-Frame-Options", "DENY",
                        "X-Content-Type-Options", "nosniff",
                        "Content-Security-Policy", "default-src 'self'; script-src 'self'");

        boolean enforcesHttps =
                standardHeaders
                        .get("Strict-Transport-Security")
                        .contains("includeSubDomains"); // true (1 year HSTS with subdomains)
        boolean preventsFraming =
                "DENY".equals(standardHeaders.get("X-Frame-Options")); // true (clickjacking protection)
        boolean preventsMimeSniffing =
                "nosniff".equals(standardHeaders.get("X-Content-Type-Options")); // true (MIME attack defense)
        boolean restrictsScripts =
                standardHeaders.get("Content-Security-Policy").contains("'self'"); // true (XSS mitigation)

        System.out.println("Enforces HTTPS (HSTS): " + enforcesHttps); // true
        System.out.println("Prevents Framing: " + preventsFraming); // true
        System.out.println("Nosniff Active: " + preventsMimeSniffing); // true
        System.out.println("CSP Restricts Scripts: " + restrictsScripts); // true
    }
}
