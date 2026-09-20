package lab.springsecurity.questions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Q06AuthenticationSchemesOverview {

    public static void main(String[] args) {
        // 1. HTTP Basic Authentication: "Basic base64(username:password)"
        String credentials = "user:password123";
        String basicAuthHeader =
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        boolean isBasic = basicAuthHeader.startsWith("Basic "); // true

        // 2. Bearer Token (JWT / OAuth2 Access Token): "Bearer <token>"
        String bearerAuthHeader = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.signature";
        boolean isBearer = bearerAuthHeader.startsWith("Bearer "); // true

        // 3. Cookie-based Session Auth: "Cookie: JSESSIONID=abc123xyz"
        String cookieHeader = "JSESSIONID=5F9A4B12C3D4E5F6";
        boolean isSessionCookie = cookieHeader.contains("JSESSIONID="); // true

        System.out.println(
                "Basic Auth scheme recognized: " + isBasic); // Basic Auth scheme recognized: true
        System.out.println(
                "Bearer Token scheme recognized: "
                        + isBearer); // Bearer Token scheme recognized: true
        System.out.println(
                "Session Cookie scheme recognized: "
                        + isSessionCookie); // Session Cookie scheme recognized: true
    }
}
