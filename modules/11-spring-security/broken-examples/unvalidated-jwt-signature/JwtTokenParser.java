package lab.springsecurity.broken.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class JwtTokenParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtClaims parseTokenClaimsUnverified(String rawToken) {
        try {
            String[] parts = rawToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            // Only decodes payload without verifying HMAC / RSA signature or checking expiry!
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
