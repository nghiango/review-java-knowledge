package lab.springsecurity.questions;

import java.util.Map;

public class Q18JwksKeyRotationMechanics {

    public static void main(String[] args) {
        // JWKS (JSON Web Key Set) exposes multiple public keys identified by 'kid' (Key ID)
        Map<String, String> jwksKeyStore =
                Map.of(
                        "key-2025-v1", "RSAPublicKey[expires:2026-12-31]",
                        "key-2026-v2", "RSAPublicKey[active:primary]");

        // Incoming token header references "kid": "key-2026-v2"
        String incomingTokenKid = "key-2026-v2";
        boolean hasMatchingKey = jwksKeyStore.containsKey(incomingTokenKid); // true
        String resolvedKey = jwksKeyStore.get(incomingTokenKid); // "RSAPublicKey[active:primary]"

        // Token signed with retired/unknown key
        String unknownKid = "key-2020-retired";
        boolean isUnknownKeyPresent = jwksKeyStore.containsKey(unknownKid); // false

        System.out.println(
                "JWKS contains active key ID: "
                        + hasMatchingKey); // JWKS contains active key ID: true
        System.out.println(
                "Resolved Public Key: "
                        + resolvedKey); // Resolved Public Key: RSAPublicKey[active:primary]
        System.out.println(
                "Retired key recognized: " + isUnknownKeyPresent); // Retired key recognized: false
    }
}
