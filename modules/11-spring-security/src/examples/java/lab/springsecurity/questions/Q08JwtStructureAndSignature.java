package lab.springsecurity.questions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Q08JwtStructureAndSignature {

    public static void main(String[] args) throws Exception {
        String secret = "secret-hmac-key-for-interview-lab-256bit-min-length";

        // Segment 1: Header (Base64Url encoded)
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String headerB64 =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

        // Segment 2: Payload Claims (Base64Url encoded)
        String payloadJson = "{\"sub\":\"user-42\",\"role\":\"ROLE_USER\",\"exp\":1790000000}";
        String payloadB64 =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // Segment 3: HMAC-SHA256 Signature over "header.payload"
        String signingInput = headerB64 + "." + payloadB64;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        String signatureB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

        String fullJwt = headerB64 + "." + payloadB64 + "." + signatureB64;
        boolean hasThreeParts = (fullJwt.split("\\.", -1).length == 3); // true

        // Constant-time signature verification
        byte[] recomputed = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        boolean signatureValid = MessageDigest.isEqual(signature, recomputed); // true

        System.out.println("JWT has 3 segments: " + hasThreeParts); // JWT has 3 segments: true
        System.out.println("HMAC Signature Valid: " + signatureValid); // HMAC Signature Valid: true
    }
}
