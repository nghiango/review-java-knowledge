package lab.springsecurity.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secretKey;

    public JwtTokenValidator() {
        this("super-secret-production-hmac-key-minimum-256-bits-length-required-12345");
    }

    public JwtTokenValidator(String secret) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public JwtClaims validateAndExtract(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("JWT token is empty");
        }

        String[] parts = rawToken.split("\\.", -1);
        if (parts.length != 3) {
            throw new BadCredentialsException("JWT token does not contain 3 segments");
        }

        String headerBase64 = parts[0];
        String payloadBase64 = parts[1];
        String signatureBase64 = parts[2];

        // 1. Verify Algorithm: Reject 'none' or unsupported algorithms
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(headerBase64);
            JsonNode headerJson = objectMapper.readTree(headerBytes);
            String alg = headerJson.path("alg").asText();
            if (!"HS256".equalsIgnoreCase(alg)) {
                throw new BadCredentialsException("Unsupported or insecure JWT algorithm: " + alg);
            }
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Malformed JWT header", e);
        }

        // 2. Cryptographic HMAC Signature Validation (constant-time)
        String signingInput = headerBase64 + "." + payloadBase64;
        byte[] expectedSignature = computeHmacSha256(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] actualSignature;
        try {
            actualSignature = Base64.getUrlDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Malformed signature encoding", e);
        }

        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new BadCredentialsException("Invalid JWT signature");
        }

        // 3. Payload and Expiration Validation
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
            JsonNode root = objectMapper.readTree(payloadBytes);

            long exp = root.path("exp").asLong();
            if (exp <= 0 || Instant.now().getEpochSecond() > exp) {
                throw new CredentialsExpiredException("JWT token has expired at " + exp);
            }

            String sub = root.path("sub").asText();
            String email = root.path("email").asText();
            String iss = root.path("iss").asText();

            List<String> roles = new ArrayList<>();
            if (root.has("roles") && root.get("roles").isArray()) {
                for (JsonNode roleNode : root.get("roles")) {
                    roles.add(roleNode.asText());
                }
            }

            return new JwtClaims(sub, email, roles, exp, iss);
        } catch (CredentialsExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to extract JWT claims", e);
        }
    }

    public String createToken(String subject, String email, List<String> roles, long ttlSeconds) {
        try {
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            long exp = Instant.now().getEpochSecond() + ttlSeconds;
            String rolesJson = objectMapper.writeValueAsString(roles);
            String payload =
                    String.format(
                            "{\"sub\":\"%s\",\"email\":\"%s\",\"roles\":%s,\"exp\":%d,\"iss\":\"lab-auth\"}",
                            subject, email, rolesJson, exp);

            String headerB64 =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(header.getBytes(StandardCharsets.UTF_8));
            String payloadB64 =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

            String signingInput = headerB64 + "." + payloadB64;
            byte[] signature = computeHmacSha256(signingInput.getBytes(StandardCharsets.UTF_8));
            String signatureB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

            return headerB64 + "." + payloadB64 + "." + signatureB64;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create test JWT", e);
        }
    }

    private byte[] computeHmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC SHA256 failure", e);
        }
    }
}
