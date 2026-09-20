package lab.springsecurity.questions;

import java.util.List;

public class Q02SecurityFilterChainArchitecture {

    public static void main(String[] args) {
        // Standard Spring Security Filter ordering sequence
        List<String> coreSecurityFilterOrder =
                List.of(
                        "DisableEncodeUrlFilter",
                        "SecurityContextHolderFilter",
                        "HeaderWriterFilter",
                        "CorsFilter",
                        "CsrfFilter",
                        "LogoutFilter",
                        "UsernamePasswordAuthenticationFilter",
                        "BearerTokenAuthenticationFilter",
                        "RequestCacheAwareFilter",
                        "SecurityContextHolderAwareRequestFilter",
                        "AnonymousAuthenticationFilter",
                        "SessionManagementFilter",
                        "ExceptionTranslationFilter",
                        "AuthorizationFilter");

        int corsIndex = coreSecurityFilterOrder.indexOf("CorsFilter"); // 3
        int csrfIndex = coreSecurityFilterOrder.indexOf("CsrfFilter"); // 4
        int authIndex =
                coreSecurityFilterOrder.indexOf("UsernamePasswordAuthenticationFilter"); // 6
        int authzFilterIndex = coreSecurityFilterOrder.indexOf("AuthorizationFilter"); // 13

        boolean corsBeforeCsrf = corsIndex < csrfIndex; // true
        boolean authBeforeAuthz = authIndex < authzFilterIndex; // true

        System.out.println(
                "CORS filter executes before CSRF filter: "
                        + corsBeforeCsrf); // CORS filter executes before CSRF filter: true
        System.out.println(
                "Authentication filter executes before Authorization filter: "
                        + authBeforeAuthz); // Authentication filter executes before Authorization
        // filter: true
    }
}
