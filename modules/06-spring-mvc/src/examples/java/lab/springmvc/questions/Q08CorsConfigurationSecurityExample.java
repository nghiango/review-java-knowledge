package lab.springmvc.questions;

import org.springframework.web.cors.CorsConfiguration;

public class Q08CorsConfigurationSecurityExample {

    public static void main(String[] args) {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("https://*.company.com");
        config.setAllowCredentials(true);
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");

        boolean allowsCredentials = Boolean.TRUE.equals(config.getAllowCredentials()); // true
        boolean hasWildcardPattern =
                config.getAllowedOriginPatterns().contains("https://*.company.com"); // true
        boolean hasWildcardOrigin =
                config.getAllowedOrigins() != null
                        && config.getAllowedOrigins().contains("*"); // false

        System.out.println(
                "Credentials allowed: "
                        + allowsCredentials
                        + ", secure patterns: "
                        + hasWildcardPattern
                        + ", insecure wildcard: "
                        + hasWildcardOrigin);
    }
}
