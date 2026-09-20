package lab.restapi.questions;

import org.springframework.http.HttpHeaders;

public class Q17RestApiSecurityHeaders {

    public static void main(String[] args) {
        HttpHeaders headers = new HttpHeaders();

        // Prevent MIME sniffing
        headers.set("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking via iframes
        headers.set("X-Frame-Options", "DENY");

        // Enforce HTTPS across all subdomains
        headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Restrict content sources
        headers.set("Content-Security-Policy", "default-src 'self'");

        boolean hasNosniff = "nosniff".equals(headers.getFirst("X-Content-Type-Options")); // true
        boolean hasHsts =
                headers.getFirst("Strict-Transport-Security").contains("max-age=31536000"); // true

        System.out.println(
                "X-Content-Type-Options set: " + hasNosniff); // X-Content-Type-Options set: true
        System.out.println(
                "Strict-Transport-Security set: " + hasHsts); // Strict-Transport-Security set: true
    }
}
