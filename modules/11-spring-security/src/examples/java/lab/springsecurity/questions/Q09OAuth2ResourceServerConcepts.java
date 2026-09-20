package lab.springsecurity.questions;

import java.util.List;
import java.util.Map;

public class Q09OAuth2ResourceServerConcepts {

    public static void main(String[] args) {
        // OAuth2 RFC 6749 Roles
        Map<String, String> roles =
                Map.of(
                        "Resource Owner", "The end-user granting access to their data",
                        "Client",
                                "The application requesting access (e.g. SPA, mobile app, backend service)",
                        "Authorization Server",
                                "Issues access tokens and ID tokens after authenticating the owner (e.g. Keycloak, Auth0, Okta)",
                        "Resource Server",
                                "The backend API validating access tokens and serving protected resources");

        // Claims in standard JWT Access Token vs OIDC ID Token
        List<String> accessTokenClaims = List.of("iss", "sub", "aud", "exp", "scope", "client_id");
        List<String> idTokenClaims =
                List.of("iss", "sub", "aud", "exp", "auth_time", "nonce", "email", "name");

        boolean isResourceServerHostingApi =
                roles.get("Resource Server").contains("backend API"); // true
        boolean idTokenContainsUserProfile = idTokenClaims.contains("email"); // true
        boolean accessTokenCarriesScope = accessTokenClaims.contains("scope"); // true

        System.out.println(
                "Resource Server validates access tokens: "
                        + isResourceServerHostingApi); // Resource Server validates access tokens:
        // true
        System.out.println(
                "OIDC ID Token carries user identity claims: "
                        + idTokenContainsUserProfile); // OIDC ID Token carries user identity
        // claims: true
        System.out.println(
                "Access token carries scopes: "
                        + accessTokenCarriesScope); // Access token carries scopes: true
    }
}
