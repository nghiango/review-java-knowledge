# Solution: Unvalidated JWT Signature & Expiration

## Annotated Code

### `JwtTokenParser.java`
```java
package lab.springsecurity.broken.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class JwtTokenParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Security issue: Accepts any arbitrary JWT payload by decoding Base64 without verifying HMAC-SHA256 or RSA digital signature (CWE-347).
    // Security issue: Attacker can forge tokens with arbitrary claims (e.g. sub: "admin", roles: ["ROLE_ADMIN"]).
    // Security issue: Omission of 'exp' expiration check allows indefinite replay of expired or revoked tokens (CWE-613).
    public JwtClaims parseTokenClaimsUnverified(String rawToken) {
        try {
            String[] parts = rawToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String jsonPayload = new String(payloadBytes, StandardCharsets.UTF_8);

            JsonNode root = objectMapper.readTree(jsonPayload);
            String sub = root.path("sub").asText();
            String email = root.path("email").asText();
            long exp = root.path("exp").asLong();

            List<String> roles = new ArrayList<>();
            if (root.has("roles") && root.get("roles").isArray()) {
                for (JsonNode roleNode : root.get("roles")) {
                    roles.add(roleNode.asText());
                }
            }

            return new JwtClaims(sub, email, roles, exp);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JWT: " + e.getMessage(), e);
        }
    }
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Unverified JWT Digital Signature | Security | Critical | Decoding the payload segment of a JWT without verifying the cryptographic signature (via shared HMAC secret or public RSA key) allows any caller to forge tokens with arbitrary subjects, authorities, and permissions (CWE-347 / OWASP API2:2023). |
| Missing Token Expiration (`exp`) Validation | Security | Critical | Failing to check the `exp` timestamp against current server time (`Instant.now().getEpochSecond()`) means expired, leaked, or revoked tokens can be replayed indefinitely (CWE-613). |
| Vulnerability to `alg: none` Attack | Security | Critical | Custom parsers that ignore the JWT header algorithm field accept unsigned tokens produced by attackers setting `"alg": "none"`. |

## Correct Implementation Reference
- [`JwtTokenValidator.java`](../../src/main/java/lab/springsecurity/jwt/JwtTokenValidator.java)
- [`SafeJwtAuthenticationFilter.java`](../../src/main/java/lab/springsecurity/jwt/SafeJwtAuthenticationFilter.java)
