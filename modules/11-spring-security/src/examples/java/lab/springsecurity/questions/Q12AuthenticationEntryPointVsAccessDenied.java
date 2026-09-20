package lab.springsecurity.questions;

import org.springframework.http.HttpStatus;

public class Q12AuthenticationEntryPointVsAccessDenied {

    public static void main(String[] args) {
        // ExceptionTranslationFilter logic:
        // Scenario 1: Anonymous / unauthenticated request fails -> AuthenticationEntryPoint
        // triggers HTTP 401 Unauthorized
        boolean isAnonymous = true;
        int statusUnauthenticated =
                isAnonymous ? HttpStatus.UNAUTHORIZED.value() : HttpStatus.FORBIDDEN.value(); // 401

        // Scenario 2: Authenticated user with insufficient privileges -> AccessDeniedHandler
        // triggers HTTP 403 Forbidden
        boolean isAuthenticated = true;
        boolean hasRequiredRole = false;
        int statusForbidden =
                (isAuthenticated && !hasRequiredRole)
                        ? HttpStatus.FORBIDDEN.value()
                        : HttpStatus.OK.value(); // 403

        System.out.println(
                "Unauthenticated request status: "
                        + statusUnauthenticated); // Unauthenticated request status: 401
        System.out.println(
                "Insufficient permission status: "
                        + statusForbidden); // Insufficient permission status: 403
    }
}
