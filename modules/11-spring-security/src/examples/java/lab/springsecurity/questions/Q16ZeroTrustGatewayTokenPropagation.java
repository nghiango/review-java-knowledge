package lab.springsecurity.questions;

import java.util.Map;

public class Q16ZeroTrustGatewayTokenPropagation {

    public static void main(String[] args) {
        // Gateway validates external OAuth2 access token and issues an internal cryptographically
        // signed assertion header
        String externalAccessToken = "Bearer eyJhbGciOiJSUzI1NiIs...";
        boolean gatewayValidated = externalAccessToken.startsWith("Bearer "); // true

        // Propagated downstream via internal mTLS / signed JWT header
        Map<String, String> downstreamHeaders =
                Map.of(
                        "X-Authenticated-User", "user-789",
                        "X-Authenticated-Roles", "ROLE_USER,order:write",
                        "X-Tenant-Id", "tenant-alpha");

        String downstreamUser = downstreamHeaders.get("X-Authenticated-User"); // "user-789"
        boolean isDownstreamAuthorized =
                downstreamHeaders.get("X-Authenticated-Roles").contains("order:write"); // true

        System.out.println(
                "Gateway validation passed: "
                        + gatewayValidated); // Gateway validation passed: true
        System.out.println(
                "Downstream identity: " + downstreamUser); // Downstream identity: user-789
        System.out.println(
                "Downstream authorization check: "
                        + isDownstreamAuthorized); // Downstream authorization check: true
    }
}
